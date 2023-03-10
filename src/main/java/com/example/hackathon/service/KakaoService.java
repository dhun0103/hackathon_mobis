package com.example.hackathon.service;

import com.example.hackathon.domain.Member;
import com.example.hackathon.domain.RefreshToken;
import com.example.hackathon.domain.SocialAccessToken;

import com.example.hackathon.dto.SocialUserInfoDto;
import com.example.hackathon.dto.TokenDto;
import com.example.hackathon.dto.UserInfoDto;
import com.example.hackathon.exception.CustomException;
import com.example.hackathon.exception.ErrorCode;
import com.example.hackathon.repository.MemberRepository;
import com.example.hackathon.repository.RefreshTokenRepository;
import com.example.hackathon.repository.SocialAccessTokenRepository;
import com.example.hackathon.security.jwt.JwtUtil;
import com.example.hackathon.security.jwt.UserDetailsImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.example.hackathon.dto.responseDto.GlobalResponseDto;


import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class KakaoService {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirect_uri;
    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String user_info_uri;
    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String token_uri;

    public final MemberRepository memberRepository;
    public final RefreshTokenRepository refreshTokenRepository;
    private final SocialAccessTokenRepository socialAccessTokenRepository;
    public final JwtUtil jwtUtil;

    public GlobalResponseDto<?> kakaoLogin(String code, HttpServletResponse response) throws JsonProcessingException {

        //??????????????? ?????? access_token ????????????
        String accessToken = issuedAccessToken(code);

        //access_token??? ?????? ????????? ??????????????????
        SocialUserInfoDto socialUserInfoDto = getKakaoUserInfo(accessToken);

        //?????????????????? ????????? ??????????????????(?????? DB??? ????????? ??????????????? ?????????)
        Member member = saveMember(socialUserInfoDto,accessToken);

        //?????? ????????? ??????
        forceLoginUser(member);

        //????????????, ????????? ?????? ?????????
        //?????? ????????? response
        createToken(member,response);

        UserInfoDto userInfoDto = new UserInfoDto(member);

        return GlobalResponseDto.success(userInfoDto, "???????????? ?????????????????????");
    }

    //??????????????? ?????? access_token ????????????
    public String issuedAccessToken(String code) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body ??????
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoClientId);
        body.add("redirect_uri", redirect_uri);
        body.add("code", code);
        body.add("client_secret", kakaoClientSecret);

        // HTTP ?????? ?????????
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest =
                new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                token_uri,
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        // HTTP ?????? (JSON) -> ????????? ?????? ??????
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        String accessToken = jsonNode.get("access_token").asText();

        return accessToken;
    }

    //access_token??? ?????? ????????? ??????????????????
    private SocialUserInfoDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {
        // HTTP Header ??????
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP ?????? ?????????
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                user_info_uri,
                HttpMethod.POST,
                kakaoUserInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long id = jsonNode.get("id").asLong();
        String nickname = jsonNode.get("properties")
                .get("nickname").asText();
        String email = jsonNode.get("kakao_account")
                .get("email").asText();

        return new SocialUserInfoDto(id, nickname, email);
    }

    //?????????????????? ????????? ??????????????????(?????? DB??? ????????? ??????????????? ?????????)
    public Member saveMember(SocialUserInfoDto socialUserInfoDto,String accessToken) {
        Member kakaoMember = memberRepository.findByEmail("k_"+socialUserInfoDto.getEmail()).orElse(null);

        //????????? ??????
        if (kakaoMember == null) {

            Member member = Member.builder().
                    email("k_" + socialUserInfoDto.getEmail())
                    .accountName(socialUserInfoDto.getNickname())
                    .isAccepted(false)
                    .isDeleted(false)
                    .build();

            memberRepository.save(member);
            return member;
        }

        SocialAccessToken socialAccessToken = new SocialAccessToken(accessToken,"k_" + socialUserInfoDto.getEmail(),"kakao");
        socialAccessTokenRepository.save(socialAccessToken);

        //????????? member ??????
        return kakaoMember;
    }

    public void forceLoginUser(Member member) {
        UserDetails userDetails = new UserDetailsImpl(member);
//        if (member.getIsDeleted().equals(true)) {
//            throw new CustomException(ErrorCode.DELETED_USER_EXCEPTION);
//        }
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public void createToken(Member member,HttpServletResponse response){
        TokenDto tokenDto = jwtUtil.createAllToken(member.getEmail());

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByAccountEmail(member.getEmail());

//        if (refreshToken.isPresent()) {
//            refreshTokenRepository.save(refreshToken.get().updateToken(tokenDto.getRefreshToken()));
//        } else {
//            RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), member.getEmail());
//            refreshTokenRepository.save(newToken);
//        }

        setHeader(response, tokenDto);
    }

    public void setHeader(HttpServletResponse response, TokenDto tokenDto) {
        response.addHeader(JwtUtil.ACCESS_TOKEN, tokenDto.getAccessToken());
        response.addHeader(JwtUtil.REFRESH_TOKEN, tokenDto.getRefreshToken());
    }
}
