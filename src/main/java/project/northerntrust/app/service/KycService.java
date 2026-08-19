package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.entity.KycProfile;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.KycStatus;
import project.northerntrust.app.entity.enums.VerificationStatus;
import project.northerntrust.app.repository.KycRepository;
import project.northerntrust.app.repository.UserRepository;

import java.util.Optional;

@Service
public class KycService {

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public MessageResponse submitKyc(String accountNumber, String bvn, String nin, String idType, String idNumber) {
        Optional<User> userOpt = userRepository.findByAccountNumber(accountNumber);
        if (userOpt.isEmpty()) {
            return new MessageResponse(false, "User not found.");
        }

        User user = userOpt.get();
        KycProfile profile = kycRepository.findByUser(user).orElse(new KycProfile());
        
        profile.setUser(user);
        profile.setBvn(bvn);
        profile.setNin(nin);
        profile.setIdType(idType);
        profile.setIdNumber(idNumber);
        profile.setVerificationStatus(VerificationStatus.PENDING);
        
        kycRepository.save(profile);
        
        user.setKycStatus(KycStatus.PENDING);
        userRepository.save(user);

        return new MessageResponse(true, "KYC documents submitted for review.");
    }

    @Transactional
    public MessageResponse approveKyc(String accountNumber) {
        Optional<User> userOpt = userRepository.findByAccountNumber(accountNumber);
        if (userOpt.isEmpty()) {
            return new MessageResponse(false, "User not found.");
        }

        User user = userOpt.get();
        Optional<KycProfile> profileOpt = kycRepository.findByUser(user);
        if (profileOpt.isEmpty()) {
            return new MessageResponse(false, "No KYC profile found for this user.");
        }

        KycProfile profile = profileOpt.get();
        profile.setVerificationStatus(VerificationStatus.VERIFIED);
        kycRepository.save(profile);

        user.setKycStatus(KycStatus.VERIFIED);
        userRepository.save(user);

        return new MessageResponse(true, "User KYC has been VERIFIED.");
    }
}
