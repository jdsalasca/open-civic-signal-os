package org.opencivic.signalos.service;

import java.util.UUID;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.ContentFollowRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.repository.VoteRepository;
import org.opencivic.signalos.web.dto.OnboardingChecklistResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingChecklistService {
    private final UserRepository userRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final SignalRepository signalRepository;
    private final VoteRepository voteRepository;
    private final ContentFollowRepository followRepository;

    public OnboardingChecklistService(
        UserRepository userRepository,
        CommunityMembershipRepository membershipRepository,
        SignalRepository signalRepository,
        VoteRepository voteRepository,
        ContentFollowRepository followRepository
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.signalRepository = signalRepository;
        this.voteRepository = voteRepository;
        this.followRepository = followRepository;
    }

    @Transactional(readOnly = true)
    public OnboardingChecklistResponse checklist(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        UUID userId = user.getId();
        boolean joined = membershipRepository.countByUserId(userId) > 0;
        boolean contributed = signalRepository.countByAuthorId(userId) > 0
            || voteRepository.countByUserId(userId) > 0;
        boolean following = followRepository.countByUserId(userId) > 0;
        return new OnboardingChecklistResponse(joined, contributed, following, joined && contributed && following);
    }
}
