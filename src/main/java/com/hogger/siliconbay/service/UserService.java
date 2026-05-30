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
        } else if (!userDTO.getPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Please provide valid password. \n " +
                    "The password must be at least 8 characters long and include at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character";
        } else {
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
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
            hibernateSession.close();
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
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();

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
            hibernateSession.close();
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
            message = "Please provide valid password. \n " +
                    "The password must be at least 8 characters long and include at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character";
        } else {
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();

            User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                    .setParameter("email", userDTO.getEmail())
                    .getSingleResultOrNull();

            if (singleUser == null) {
                message = "Account not found. Please register first!";
            } else if (!singleUser.getPassword().equals(userDTO.getPassword())) {
                message = "Incorrect password. Please try again!";

                hibernateSession.close();
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

            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
