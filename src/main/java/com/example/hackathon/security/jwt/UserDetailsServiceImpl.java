package com.example.hackathon.security.jwt;

import com.example.hackathon.domain.Member;
import com.example.hackathon.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Member member = memberRepository.findByEmail(email).orElseThrow(
                () -> new RuntimeException("NOT FOUNT ACCOUNT")  //커스텀 예외처리 해야함
        );

        UserDetailsImpl userDetails = new UserDetailsImpl();
        userDetails.setAccount(member);

        return userDetails;
    }
}
