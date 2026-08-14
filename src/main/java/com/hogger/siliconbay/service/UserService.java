package com.hogger.siliconbay.service;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.UserDTO;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.User;
import com.hogger.siliconbay.mail.VerificationMail;
import com.hogger.siliconbay.provider.MailServiceProvider;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import com.hogger.siliconbay.util.JwtUtil;
import com.hogger.siliconbay.validation.Validator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Context;

public class UserService {
    public String addNewUser(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();

        boolean status = false;
        String message;

        if (userDTO.getFirstName() == null) {
            message = "First name is required!";
        } else if (userDTO.getFirstName().isBlank()) {
            message = "First name can not be empty!";
        } else if (userDTO.getLastName() == null) {
            message = "Last name is required!";
        } else if (userDTO.getLastName().isBlank()) {
            message = "Last name can not be empty!";
        } else if (userDTO.getEmail() == null) {
            message = "Email is required!";
        } else if (userDTO.getEmail().isBlank()) {
            message = "Email can not be empty!";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide valid email address!";
        } else if (userDTO.getPassword() == null) {
            message = "Password is required!";
        } else if (userDTO.getPassword().isBlank()) {
            message = "Password can not be empty!";
        } else {
            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                        .setParameter("email", userDTO.getEmail())
                        .getSingleResultOrNull();

                if (singleUser != null) { // Already exists
                    message = "This email already exists! Please use another email";
                } else {
                    User u = new User();
                    u.setFirstName(userDTO.getFirstName());
                    u.setLastName(userDTO.getLastName());
                    u.setEmail(userDTO.getEmail());
                    u.setPassword(userDTO.getPassword());
                    u.setRole("USER"); // Set default role

                    String verificationCode = AppUtil.generateCode();

                    u.setVerificationCode(verificationCode);

                    Status pendingStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("value", String.valueOf(Status.Type.PENDING)).getSingleResult();

                    u.setStatus(pendingStatus);

                    Transaction transaction = hibernateSession.beginTransaction();

                    try {
                        hibernateSession.persist(u);

                        // Send verification email
                        VerificationMail verificationMail = new VerificationMail(u.getEmail(), verificationCode);
                        MailServiceProvider.getInstance().sendMail(verificationMail);

                        transaction.commit();

                        status = true;
                        message = "Account created successfully. Verification code has been sent to the your email. " +
                                "Please verify it for activate your account!";


                    } catch (HibernateException e) {
                        transaction.rollback();
                        message = "Account creation failed. Please try again!";
                    } catch (RuntimeException e) {
                        transaction.rollback();
                        message = "Account creation failed. Verification email could not be sent!";
                    }

                }
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String verifyUserAccount(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;


        if (userDTO.getEmail() == null) {
            message = "Email is required!";
        } else if (userDTO.getEmail().isBlank()) {
            message = "Email address can not be empty!";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide valid email address!";
        } else if (userDTO.getVerificationCode() == null) {
            message = "Verification is required!";
        } else if (userDTO.getVerificationCode().isBlank()) {
            message = "Verification code can not be empty!";
        } else if (!userDTO.getVerificationCode().matches(Validator.VERIFICATION_CODE_VALIDATION)) {
            message = "Please provide valid verification code!. Verification code must have 6 digits";
        } else {
            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

                User user = hibernateSession.createQuery("FROM User u WHERE u.email=:email AND u.verificationCode=:verificationCode", User.class)
                        .setParameter("email", userDTO.getEmail())
                        .setParameter("verificationCode", userDTO.getVerificationCode())
                        .getSingleResultOrNull();

                if (user == null) {
                    message = "Account not found. Please register first!";
                } else {
                    Status verifiedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("value", String.valueOf(Status.Type.VERIFIED))
                            .getSingleResult();

                    if (user.getStatus() != null && verifiedStatus.getValue().equals(user.getStatus().getValue())) {
                        message = "Account already verified!";
                    } else {
                        Transaction transaction = hibernateSession.beginTransaction();

                        try {
                            user.setStatus(verifiedStatus);
                            user.setVerificationCode("");

                            transaction.commit();

                            status = true;
                            message = "Account verification completed!";
                        } catch (HibernateException e) {
                            transaction.rollback();
                            message = "Something went wrong. Verification process failed!";
                        }
                    }
                }
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String userLogin(UserDTO userDTO, @Context HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (userDTO.getEmail() == null) {
            message = "Email is required!";
        } else if (userDTO.getEmail().isBlank()) {
            message = "Email address can not be empty!";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide valid email address!";
        } else if (userDTO.getPassword() == null) {
            message = "Password is required!";
        } else if (userDTO.getPassword().isBlank()) {
            message = "Password can not be empty!";
        } else if (!userDTO.getPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Please provide valid password. The password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, one digit, and one special character";
        } else {
            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

                User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                        .setParameter("email", userDTO.getEmail())
                        .getSingleResultOrNull();

                if (singleUser == null) {
                    message = "Account not found. Please register first!";
                } else if (!singleUser.getPassword().equals(userDTO.getPassword())) {
                    message = "Incorrect password. Please try again!";

                    responseObject.addProperty("status", status);
                    responseObject.addProperty("message", message);
                    return AppUtil.GSON.toJson(responseObject);

                } else {
                    Status verifiedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("value", String.valueOf(Status.Type.VERIFIED))
                            .getSingleResult();

                    if (singleUser.getStatus() == null || !verifiedStatus.getValue().equals(singleUser.getStatus().getValue())) {
                        message = "Your account is not verified. Please verify first!";
                    } else {
                        String role = singleUser.getRole() == null ? "USER" : singleUser.getRole();
                        String token = JwtUtil.generateToken(singleUser.getEmail(), singleUser.getId(), role);

                        HttpSession session = request.getSession(true);
                        session.setAttribute("user", singleUser.getEmail());
                        session.setAttribute("userId", singleUser.getId());
                        session.setAttribute("role", role);

                        status = true;
                        message = "Login successful";

                        responseObject.addProperty("token", token);

                        JsonObject userObject = new JsonObject();
                        userObject.addProperty("id", singleUser.getId());
                        userObject.addProperty("email", singleUser.getEmail());
                        userObject.addProperty("firstName", singleUser.getFirstName());
                        userObject.addProperty("lastName", singleUser.getLastName());
                        userObject.addProperty("role", role);

                        responseObject.add("user", userObject);
                    }
                }
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getMyProfile(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            User user = hibernateSession.get(User.class, userId);
            if (user == null) {
                return error("Account not found");
            }

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Profile found successfully");
            responseObject.add("user", buildUserProfileJson(user));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String updateMyProfile(String jsonData, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }

        UserDTO dto = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        if (dto == null) {
            return error("Invalid request");
        }

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            User user = hibernateSession.get(User.class, userId);

            if (user == null) {
                transaction.rollback();
                return error("Account not found");
            }

            if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) {
                user.setFirstName(dto.getFirstName().trim());
            }
            if (dto.getLastName() != null && !dto.getLastName().isBlank()) {
                user.setLastName(dto.getLastName().trim());
            }
            if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                String nextEmail = dto.getEmail().trim();
                if (!nextEmail.matches(Validator.EMAIL_VALIDATION)) {
                    transaction.rollback();
                    return error("Please provide valid email address");
                }

                User existing = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                        .setParameter("email", nextEmail)
                        .getSingleResultOrNull();
                if (existing != null && existing.getId() != user.getId()) {
                    transaction.rollback();
                    return error("This email already exists! Please use another email");
                }
                user.setEmail(nextEmail);
            }

            boolean wantsPasswordChange = (dto.getNewPassword() != null && !dto.getNewPassword().isBlank())
                    || (dto.getConfirmPassword() != null && !dto.getConfirmPassword().isBlank());
            if (wantsPasswordChange) {
                if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                    transaction.rollback();
                    return error("Current password is required");
                }
                if (!user.getPassword().equals(dto.getPassword())) {
                    transaction.rollback();
                    return error("Current password is incorrect");
                }
                if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
                    transaction.rollback();
                    return error("New password is required");
                }
                if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
                    transaction.rollback();
                    return error("New password and confirmation do not match");
                }
                if (!dto.getNewPassword().matches(Validator.PASSWORD_VALIDATION)) {
                    transaction.rollback();
                    return error("Please provide valid password. The password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, one digit, and one special character");
                }
                user.setPassword(dto.getNewPassword());
            }

            transaction.commit();

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("user", user.getEmail());
                session.setAttribute("userId", user.getId());
                session.setAttribute("role", user.getRole());
            }

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Profile updated successfully");
            responseObject.add("user", buildUserProfileJson(user));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    private JsonObject buildUserProfileJson(User user) {
        JsonObject userObject = new JsonObject();
        userObject.addProperty("id", user.getId());
        userObject.addProperty("firstName", user.getFirstName());
        userObject.addProperty("lastName", user.getLastName());
        userObject.addProperty("email", user.getEmail());
        userObject.addProperty("role", user.getRole());
        userObject.addProperty("status", user.getStatus() == null ? null : user.getStatus().getValue());
        return userObject;
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
