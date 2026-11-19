package com.example.demo;

import java.util.List;

/**
 * 회원가입 폼 (어노테이션 없음 — 규칙은 {@link JoinController#RULES}에서 S2 로 정의).
 */
public class JoinForm {
    private String name;
    private String email;
    private String mobile;
    private String password;
    private String passwordConfirm;
    private String memberType;
    private String bizNo;
    private List<String> interests;
    private boolean agree;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordConfirm() { return passwordConfirm; }
    public void setPasswordConfirm(String passwordConfirm) { this.passwordConfirm = passwordConfirm; }
    public String getMemberType() { return memberType; }
    public void setMemberType(String memberType) { this.memberType = memberType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }
    public boolean isAgree() { return agree; }
    public void setAgree(boolean agree) { this.agree = agree; }
}
