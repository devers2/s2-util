package com.example.demo;

import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import io.github.devers2.s2util.validation.S2BeanValidation;
import io.github.devers2.s2util.validation.S2RuleType;
import io.github.devers2.s2util.validation.S2Validator;
import io.github.devers2.s2util.validation.spring.S2SpringValidator;

import jakarta.validation.Valid;

/**
 * 방식 2 — 이미 Bean Validation 을 쓰는 폼에 얹기. 서버는 {@code @Valid} 그대로, 브라우저 규칙은 어노테이션에서 만들고 Bean Validation
 * 에 없는 규칙만 S2 로 더한다.
 */
@Controller
public class ProfileController {

    private static final Map<String, String> LABELS = Map.of("nickname", "닉네임", "age", "나이", "zip", "우편번호", "tags", "태그");

    /** Bean Validation 에 없는 규칙 (서버: @Valid 흐름에 추가) */
    static final S2Validator<ProfileForm> EXTRA = S2Validator.<ProfileForm>builder()
            .field("vipCode", "VIP 코드").when("type", "VIP").rule(S2RuleType.REQUIRED)
            .build();

    /** 브라우저 규칙: 어노테이션에서 변환 + 같은 S2 규칙 */
    static final S2Validator<ProfileForm> BROWSER = S2BeanValidation.of(ProfileForm.class)
            .labels(LABELS)
            .toBuilder()
            .field("vipCode", "VIP 코드").when("type", "VIP").rule(S2RuleType.REQUIRED)
            .build();

    @InitBinder("profileForm")
    void initBinder(WebDataBinder binder) {
        binder.addValidators(S2SpringValidator.of(ProfileForm.class, EXTRA)); // @Valid 때 Bean Validation 과 함께 실행
    }

    @GetMapping("/profile")
    public String form(Model model, Locale locale) {
        model.addAttribute("profileForm", new ProfileForm());
        model.addAttribute("rules", BROWSER.getRulesJson(locale));
        return "profile";
    }

    @PostMapping("/profile")
    public String submit(@Valid @ModelAttribute ProfileForm profileForm, BindingResult result, Model model, Locale locale) {
        if (result.hasErrors()) { // Bean Validation 오류와 S2 오류가 함께
            model.addAttribute("rules", BROWSER.getRulesJson(locale));
            return "profile";
        }
        model.addAttribute("message", profileForm.getNickname() + " 프로필이 저장되었습니다.");
        return "done";
    }
}
