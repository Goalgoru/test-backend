package com.sesac.foodtruckuser.ui.controller;

import com.sesac.foodtruckuser.application.security.jwt.JwtTokenProvider;
import com.sesac.foodtruckuser.application.service.UserService;
import com.sesac.foodtruckuser.application.service.redis.RedisService;
import com.sesac.foodtruckuser.infrastructure.persistence.mysql.entity.User;
import com.sesac.foodtruckuser.infrastructure.persistence.mysql.repository.UserRepository;
import com.sesac.foodtruckuser.ui.dto.*;
import com.sesac.foodtruckuser.ui.dto.request.UserRequestDto;
import com.sesac.foodtruckuser.ui.dto.response.UserResponseDto;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;

@Slf4j
@RequiredArgsConstructor
@RestController
public class UserApiController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final RedisService redisService;
    private final Response response;

    /**
     * ?????? ????????????
     * @author jaemin
     * @version 1.0.0
     * ????????? 2022-03-26
    **/
    @Operation(summary = "????????????", responses = {
                    @ApiResponse(
                            responseCode = "200", description = "ok"
                            , content = @Content(schema = @Schema(implementation = UserResponseDto.JoinUserDto.class)))
            })
    @io.swagger.annotations.ApiResponses(
            @io.swagger.annotations.ApiResponse(
                    response = UserResponseDto.JoinUserDto.class, message = "ok", code=200)
    )
    @ApiOperation(value = "?????? ????????????")
    @PostMapping("/users/v1/join")
    public ResponseEntity<?> signUpUser(@Valid @RequestBody UserRequestDto.JoinUserDto userDto, BindingResult results) {

        log.info("?????? ????????????");

        // validation ??????
        if (results.hasErrors()) {
            return response.invalidFields(Helper.refineErrors(results));
        }

        return userService.signUpUser(userDto);
    }

    /**
     * ?????? ????????????
     * @author jaemin
     * @version 1.0.0
     * ????????? 2022-03-29
    **/
    @ApiOperation(value = "?????? ????????????")
    @PostMapping("/users/v1/owner/join")
    public ResponseEntity<?> signUpManager(@Valid @RequestBody UserRequestDto.JoinManagerDto managerDto, BindingResult results) {

        log.info("?????? ????????????");

        // validation ??????
        if (results.hasErrors()) {
            return response.invalidFields(Helper.refineErrors(results));
        }

        return userService.signUpManager(managerDto);
    }

    /**
     * ?????????
     * ?????????, ??????
     * @author jaemin
     * @version 1.0.0
     * ????????? 2022-03-27
    **/
    // ?????????
    @ApiOperation(value = "?????????")
    @PostMapping("/users/v1/logins")
    public ResponseEntity<?> authorize(@RequestBody UserRequestDto.LoginUserDto requestUser) {

        log.info("????????? request");

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(requestUser.getEmail(), requestUser.getPassword());

        // loadUserByUsername ??????????????? ???????????? user ????????? Authentication ?????? ??????
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        // ????????? ????????? SecurityContext ??? ??????
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ????????? ????????? TokenProvider.createToken ???????????? ?????? jwt ????????? ??????
        String accessToken = jwtTokenProvider.createToken(authentication, false);
        String refreshToken = jwtTokenProvider.createToken(authentication, true);

        // redis ??? ??????
        redisService.setRefreshToken(requestUser.getEmail(), refreshToken);

        // id ??????
        String email = authentication.getName();
        User findUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("email ???????????? ????????? ???????????? ???????????? " + email));
        Long userId = findUser.getId();

        HttpHeaders httpHeaders = new HttpHeaders();

        // Header ??? ??????
        httpHeaders.add("Authorization", "Bearer " + accessToken);

        // jwt ??????
        return response.successToken(new UserResponseDto.TokenDto(accessToken, refreshToken, userId), "???????????? ??????????????????.", httpHeaders, HttpStatus.OK);
    }

    /**
     * ????????????
     * @author jaemin
     * @version 1.0.0
     * ????????? 2022-03-29
    **/
    @ApiOperation(value = "????????????")
    @PostMapping("/users/v1/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody UserRequestDto.LogoutUserDto logoutDto, BindingResult results) {

        log.info("????????????");

        // validation ??????
        if (results.hasErrors()) {
            return response.invalidFields(Helper.refineErrors(results));
        }

        return userService.logout(logoutDto);
    }

    /**
     * access token ??????
     * ?????? ?????? ??????, ???????????? ?????? ?????? - jaemin
     * @author jjaen
     * @version 1.0.0
     * ????????? 2022-03-29
    **/
    @ApiOperation(value = "?????? ??????")
    @PostMapping("/users/v1/refresh")
    public ResponseEntity<?> updateRefreshToken(@Valid @RequestBody UserRequestDto.UpdateTokenDto updateTokenDto, BindingResult results) {

        log.info("Access Token ??????");

        // validation ??????
        if (results.hasErrors()) {
            return response.invalidFields(Helper.refineErrors(results));
        }

        return userService.updateRefreshToken(updateTokenDto);
    }

    /**
     * ?????? ?????? ??????(????????? ??????)
     * ?????????, ??????
     * @author jaemin
     * @version 1.0.0
     * ????????? 2022-03-28
     **/
    @ApiOperation(value = "????????? ??????")
    @PatchMapping("/users/v1/mypage/name")
    public ResponseEntity<?> updateUsername(Principal principal,
                                      @Valid @RequestBody UserRequestDto.UpdateNameDto updateNameDto,
                                      BindingResult results) {
        log.info("?????????????????? - ?????????");

        // validation ??????
        if (results.hasErrors()) {
            return response.invalidFields(Helper.refineErrors(results));
        }

        return userService.updateUsername(principal.getName(), updateNameDto);
    }

    /**
     * ?????? ?????? ??????(???????????? ??????)
     * ?????????, ??????
     * @author jaemin
     * @version 1.0.0
     * ????????? 2022-03-29
    **/
    @ApiOperation(value = "???????????? ??????")
    @PatchMapping("/users/v1/mypage/password")
    public ResponseEntity<?> updatePassword(Principal principal,
                                      @Valid @RequestBody UserRequestDto.UpdatePwDto updatePwDto,
                                      BindingResult results) {
        log.info("?????????????????? - ???????????? ??????");

        // validation ??????
        if (results.hasErrors()) {
            return response.invalidFields(Helper.refineErrors(results));
        }

        return userService.updatePassword(principal.getName(), updatePwDto);
    }

    /**
     * ????????? ?????? ??????
     * @author jaemin
     * @version 1.0.0
     * ????????? 2022-03-29
     **/
    @ApiOperation(value = "????????? ?????? ??????")
    @PostMapping("/users/v1/validation/email")
    public ResponseEntity<?> validateDuplicateEmail(@RequestBody UserRequestDto.DuplicateEmail duplicateEmail) {

        return userService.validateDuplicateEmail(duplicateEmail.getEmail());
    }

    /**
     * ????????? ?????? ??????
     * @author jaemin
     * @version 1.0.0
     * ????????? 2022-03-29
    **/
    @ApiOperation(value = "????????? ?????? ??????")
    @PostMapping("/users/v1/validation/name")
    public ResponseEntity<?> validateDuplicateUsername(@Valid @RequestBody UserRequestDto.UpdateNameDto updateNameDto, BindingResult results) {

        // validation ??????
        if (results.hasErrors()) {
            return response.invalidFields(Helper.refineErrors(results));
        }
        return userService.validateDuplicateUser(updateNameDto.getUsername());
    }
}
