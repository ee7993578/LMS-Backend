package com.learningJWT.LearningTemplate.Services.Impl;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Mapper.LibraryMapper;
import com.learningJWT.LearningTemplate.Mapper.SubscriptionMapper;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Model.Subscription;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SubscriptionDTO;
import com.learningJWT.LearningTemplate.Repository.LibraryPlanRepository;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.SubscriptionRepository;
import com.learningJWT.LearningTemplate.Services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
     private final LibraryRepository libraryRepository;
     private final LibraryPlanRepository libraryPlanRepository;
     private final UserRepository userRepository;
     private final StudentRepository studentRepository;

    @Override
    public SubscriptionDTO createSubscription(Long libraryId, Long planId) throws Exception {

        Library library = libraryRepository.findById(libraryId).orElseThrow(
                ()-> new Exception("Library Not found")
        );

        LibraryPlan libraryPlan = libraryPlanRepository.findById(planId).orElseThrow(
                ()-> new Exception("Plan Not found")
        );

        Subscription subscription = new Subscription();
        subscription.setLibrary(library);
        subscription.setLibraryPlan(libraryPlan);
        subscription.setPlanName(libraryPlan.getPlanName());
        subscription.setStatus(Status.ACTIVE);
        subscription.setBufferAllowed(libraryPlan.getBufferStudent());
        subscription.setStudentLimit(libraryPlan.getNoOfStudent());
        subscription.setPricePerMonth(libraryPlan.getPlanPrice());
        subscription.setBillingStart(LocalDate.now());
        subscription.setBillingEnd(LocalDate.now().plusDays(libraryPlan.getNoOfDays()));
        subscription.setBufferExpiryDays(3);
        //Agar hum grace date ko change karna chahenge to kar sakte hain
        subscription.setGraceEndDate(LocalDate.now().plusDays(libraryPlan.getNoOfDays()+7));
         Subscription saved= subscriptionRepository.save(subscription);

        return SubscriptionMapper.todto(saved);
    }

    @Override
    public SubscriptionDTO upgradePlan(Long libraryId, Long newPlanId) throws Exception {
        Library library = libraryRepository.findById(libraryId).orElseThrow(
                () -> new Exception("Library Not found")
        );

        LibraryPlan newPlan = libraryPlanRepository.findById(newPlanId).orElseThrow(
                () -> new Exception("Plan Not found")
        );

        Subscription currentSubscription = subscriptionRepository.findByLibraryId(libraryId);
        if (currentSubscription == null) {
            throw new Exception("No subscription found for this library");
        }

        if (currentSubscription.getLibraryPlan().getPlanId().equals(newPlanId)) {
            throw new Exception("You already have this plan");
        }

        return getSubscriptionDTO(newPlan, currentSubscription);
    }


    @Override
    public SubscriptionDTO renewSubscription(Long libraryId) throws Exception {

        Library library = libraryRepository.findById(libraryId).orElseThrow(
                () -> new Exception("Library Not found")
        );

        Long planId = library.getLibraryPlan().getPlanId();

        LibraryPlan oldPlan = libraryPlanRepository.findById(planId).orElseThrow(
                () -> new Exception("Plan Not found")
        );

        Subscription currentSubscription = subscriptionRepository.findByLibraryId(libraryId);
        if (currentSubscription == null) {
            throw new Exception("No subscription found for this library");
        }

        return getSubscriptionDTO(oldPlan, currentSubscription);
    }

    private SubscriptionDTO getSubscriptionDTO(LibraryPlan oldPlan, Subscription currentSubscription) {
        LocalDate today = LocalDate.now();
        LocalDate oldEnd = currentSubscription.getBillingEnd();

        long remainingDays = 0;
        if (oldEnd != null && oldEnd.isAfter(today)) {
            long diff = java.time.temporal.ChronoUnit.DAYS.between(today, oldEnd);
            remainingDays = Math.max(diff, 0);
        }
        currentSubscription.setLibraryPlan(oldPlan);
        currentSubscription.setPlanName(oldPlan.getPlanName());
        currentSubscription.setStatus(Status.ACTIVE);
        currentSubscription.setBufferAllowed(oldPlan.getBufferStudent());
        currentSubscription.setStudentLimit(oldPlan.getNoOfStudent());
        currentSubscription.setPricePerMonth(oldPlan.getPlanPrice());
        currentSubscription.setBillingStart(today);
        currentSubscription.setBillingEnd(today.plusDays(oldPlan.getNoOfDays() + remainingDays));
        currentSubscription.setBufferExpiryDays(3);
        currentSubscription.setGraceEndDate(today.plusDays(oldPlan.getNoOfDays() + remainingDays+7));
        Subscription saved = subscriptionRepository.save(currentSubscription);
        return SubscriptionMapper.todto(saved);
    }

    @Override
    public boolean isSubscriptionActive(Long libraryId) throws Exception {

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);

        if (subscription == null) {
            throw new Exception("Subscription not found for the library");
        }

        LocalDate today = LocalDate.now();
        if (subscription.getBillingEnd() != null &&
                subscription.getBillingEnd().isBefore(today)) {
            return false;
        }
        if (subscription.getStatus() == Status.GRACE) {
            return false;
        }
        return subscription.getStatus() == Status.ACTIVE;
    }


    @Override
    public void handleBufferExceeded(Long libraryId) {

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);

        if (subscription == null) {
            throw new RuntimeException("Subscription not found for library");
        }

        int limit = subscription.getStudentLimit();
        int buffer = subscription.getBufferAllowed();
        int maxAllowed = limit + buffer;

        int totalStudents = studentRepository.countByLibraryId(libraryId);
        if (totalStudents > maxAllowed) {
            Library library = subscription.getLibrary();
            library.setStatus(Status.EXCEEDED);
            libraryRepository.save(library);
        }
    }
//    Mid night scheduler to handle Buffer Exceeded
//    @Scheduled(cron = "0 0 0 * * *")
//    public void checkAllLibrariesBuffer() {
//        List<Subscription> all = subscriptionRepository.findAll();
//        for (Subscription s : all) {
//            handleBufferExceeded(s.getLibrary().getId());
//        }
//    }


    @Override
    public void deleteExpiredBufferStudents() {

    }

    @Override
    public void deactivateExpiredSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Subscription> activeSubs = subscriptionRepository.findByStatus(Status.ACTIVE);
        for (Subscription sub : activeSubs) {
            LocalDate billingEnd = sub.getBillingEnd();
            if (billingEnd != null && billingEnd.isBefore(today)) {
                sub.setStatus(Status.EXPIRED);
                //Agar hum handle nahin karna chahte hain grace end date wo hum yahan bhi kar sakte hain set
//                sub.setGraceEndDate(billingEnd.plusDays(7));
                Library lib = sub.getLibrary();
                lib.setStatus(Status.GRACE);
                libraryRepository.save(lib);
                subscriptionRepository.save(sub);
            }
        }
    }

    @Override
    public void deleteLibrariesAfterGracePeriod() {

        LocalDate today = LocalDate.now();


        List<Subscription> graceSubscriptions =
                subscriptionRepository.findByStatus(Status.GRACE);

        for (Subscription subscription : graceSubscriptions) {

            LocalDate graceEnd = subscription.getGraceEndDate();

            // ✅ Step 2: Skip if graceEndDate is null or still in grace period
            if (graceEnd == null || !graceEnd.isBefore(today)) {
                continue;
            }

            // ✅ Step 3: grace period over → delete library + all related data
            Library library = subscription.getLibrary();
            Long libraryId = library.getId();


            userRepository.deleteByLibraryId(libraryId);
            libraryRepository.delete(library);
            subscriptionRepository.delete(subscription);
        }
    }


    @Override
    public List<LibraryDTO> getAllLibraryBySubscriptionStatus(Status status) {

        return subscriptionRepository.findByStatus(status)
                .stream()
                .map(sub -> LibraryMapper.toDTO(sub.getLibrary()))
                .toList();
    }


    @Override
    public SubscriptionDTO getSubscriptionByLibraryId(Long libraryId) throws Exception {

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);

        if (subscription == null) {
            throw new Exception("Subscription not found for the library");
        }

        return SubscriptionMapper.todto(subscription);
    }

}
