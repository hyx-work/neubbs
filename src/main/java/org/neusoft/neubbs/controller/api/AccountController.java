package org.neusoft.neubbs.controller.api;

import org.neusoft.neubbs.constant.ajax.AjaxRequestStatus;
import org.neusoft.neubbs.constant.api.ApiMessage;
import org.neusoft.neubbs.constant.api.ParamConst;
import org.neusoft.neubbs.constant.api.SetConst;
import org.neusoft.neubbs.constant.secret.SecretInfo;
import org.neusoft.neubbs.controller.annotation.AccountActivation;
import org.neusoft.neubbs.controller.annotation.LoginAuthorization;
import org.neusoft.neubbs.dto.PageJsonDTO;
import org.neusoft.neubbs.dto.PageJsonListDTO;
import org.neusoft.neubbs.entity.UserDO;
import org.neusoft.neubbs.service.ICaptchaService;
import org.neusoft.neubbs.service.IEmailService;
import org.neusoft.neubbs.service.IFtpService;
import org.neusoft.neubbs.service.IHttpService;
import org.neusoft.neubbs.service.IParamCheckService;
import org.neusoft.neubbs.service.IRandomService;
import org.neusoft.neubbs.service.IRedisService;
import org.neusoft.neubbs.service.ISecretService;
import org.neusoft.neubbs.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 *   账户 api
 *      + 获取用户信息
 *      + 获取用户激活状态
 *      + 获取所有主动关注人信息
 *      + 获取所有被关注人信息
 *      + 登录
 *      + 注销
 *      + 注册
 *      + 修改用户基本信息
 *      + 修改密码
 *      + 修改邮箱
 *      + 账户激活（发送激活 email）
 *      + 激活账户（验证 token）
 *      + 图片验证码（页面生成图片）
 *      + 检查验证码（比较用户输入是否与图片一致）
 *      + 忘记密码（发送临时密码 email）
 *      + 关注用户
 *
 * @author Suvan
 */
@Controller
@RequestMapping("/api/account")
public final class AccountController {

    private final IParamCheckService paramCheckService;
    private final IUserService userService;
    private final ISecretService secretService;
    private final IRedisService redisService;
    private final IFtpService ftpService;
    private final IHttpService httpService;
    private final IEmailService emailService;
    private final ICaptchaService captchaService;
    private final IRandomService randomService;


    /**
     * Constructor（自动注入）
     */
    @Autowired
    private AccountController(IParamCheckService paramCheckService, IUserService userService,
                              ISecretService secretService, IRedisService redisService, IFtpService ftpService,
                              IHttpService httpService, IEmailService emailService,
                              ICaptchaService captchaService, IRandomService randomService) {
        this.paramCheckService = paramCheckService;
        this.userService = userService;
        this.secretService = secretService;
        this.redisService = redisService;
        this.ftpService = ftpService;
        this.httpService = httpService;
        this.emailService = emailService;
        this.captchaService = captchaService;
        this.randomService = randomService;
    }

    /**
     * 获取用户信息（AccountController 默认访问）
     *
     * @param username 用户名
     * @param email 用户邮箱
     * @param request http请求
     * @return PageJsonDTO 页面JSON传输对象
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    public PageJsonDTO account(@RequestParam(value = "username", required = false) String username,
                               @RequestParam(value = "email", required = false) String email,
                               HttpServletRequest request) {
        paramCheckService.paramsNotNull(username, email);
        if (username != null) {
            String paramType = paramCheckService.getUsernameParamType(username);
            paramCheckService.check(paramType, username);
        } else {
            paramCheckService.check(ParamConst.EMAIL, email);
        }

        return userService.isUserExist(username, email)
                ? new PageJsonDTO(AjaxRequestStatus.SUCCESS, userService.getUserInfoToPageModelMap(username, email))
                : new PageJsonDTO(AjaxRequestStatus.FAIL);
    }

    /**
     * 获取用户激活状态
     *
     * @param username 用户名
     * @return PageJsonDTO 页面JSON传输对象
     */
    @RequestMapping(value = "/state", method = RequestMethod.GET)
    @ResponseBody
    public PageJsonDTO accountState(@RequestParam(value = "username", required = false) String username) {
        paramCheckService.check(ParamConst.USERNAME, username);
        return new PageJsonDTO(userService.isUserActivatedByName(username));
    }

    /**
     * 获取用户所有主动关注人信息
     *
     * @param userId 用户id
     * @return PageJsonListDTO 页面JSON列表传输对象
     */
    @RequestMapping(value = "/following", method = RequestMethod.GET)
    @ResponseBody
    public PageJsonListDTO followingList(@RequestParam(value = "userid", required = false) String userId) {
        paramCheckService.check(ParamConst.USER_ID, userId);
        return new PageJsonListDTO(AjaxRequestStatus.SUCCESS,
                userService.listAllFollowingUserInfoToPageModelList(Integer.valueOf(userId)));
    }

    /**
     * 获取用户所有被关注信息
     *
     * @param userId 用户id
     * @return PageJsonListDTO 页面JSON列表传输对象
     */
    @RequestMapping(value = "/followed", method = RequestMethod.GET)
    @ResponseBody
    public PageJsonListDTO followedList(@RequestParam(value = "userid", required = false) String userId) {
        paramCheckService.check(ParamConst.USER_ID, userId);
        return new PageJsonListDTO(AjaxRequestStatus.SUCCESS,
                userService.listAllFollowedUserInfoToPageModelList(Integer.valueOf(userId)));
    }

    /**
     * 登录
     *
     * @param requestBodyParamsMap  request-body内JSON数据
     * @param request http请求
     * @param response http响应
     * @return PageJsonDTO 页面JSON传输对象
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public PageJsonDTO login(@RequestBody Map<String, Object> requestBodyParamsMap,
                                 HttpServletRequest request, HttpServletResponse response) {
        String username = (String) requestBodyParamsMap.get(ParamConst.USERNAME);
        String password = (String) requestBodyParamsMap.get(ParamConst.PASSWORD);

        paramCheckService.check(ParamConst.USERNAME, username);
        paramCheckService.check(ParamConst.PASSWORD, password);

        //database login authenticate
        UserDO user = userService.loginAuthenticate(username, password);

        //jwt secret user information, save authentication to cookie
        String authentication = secretService.jwtCreateTokenByUser(user);
        httpService.saveAuthenticationCookie(response, authentication);

        //re-count login user
        request.getSession().invalidate();

        //response model -> include authentication and state
        Map<String, Object> modelJsonMap = new LinkedHashMap<>(SetConst.LENGTH_TWO);
            modelJsonMap.put(ParamConst.AUTHENTICATION, authentication);
            modelJsonMap.put(ParamConst.STATE, userService.isUserActivatedByState(user.getState()));
        return new PageJsonDTO(AjaxRequestStatus.SUCCESS, modelJsonMap);
    }

    /**
     * 注销
     *
     * @param request http请求
     * @param response http响应
     * @return PageJsonDTO 页面JSON传输对象
     */
    @LoginAuthorization
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    @ResponseBody
    public PageJsonDTO logout(HttpServletRequest request, HttpServletResponse response) {
        httpService.removeCookie(request, response, ParamConst.AUTHENTICATION);

        //re-count login user
        request.getSession().invalidate();

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS);
    }

    /**
     * 注册
     *
     * @param requestBodyParamsMap request-body内JSON数据
     * @return PageJsonDTO 页面JSON传输对象
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public PageJsonDTO register(@RequestBody Map<String, Object> requestBodyParamsMap) {
        String username = (String) requestBodyParamsMap.get(ParamConst.USERNAME);
        String password = (String) requestBodyParamsMap.get(ParamConst.PASSWORD);
        String email = (String) requestBodyParamsMap.get(ParamConst.EMAIL);

        paramCheckService.check(ParamConst.USERNAME, username);
        paramCheckService.check(ParamConst.PASSWORD, password);
        paramCheckService.check(ParamConst.EMAIL, email);

        //database register user
        UserDO newRegisterUser = userService.registerUser(username, password, email);

        //craete user person directory on cloud ftp server
        ftpService.registerUserCreatePersonDirectory(newRegisterUser);

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS, userService.getUserInfoMapByUser(newRegisterUser));
    }

    /**
     * 修改用户基本信息
     *      - sex 不能为空，且只能为 0 or 1
     *      - birthday, position, description 允许 "",但是不能为 null
     *
     * @param requestBodyParamsMap request-body内JSON数据
     * @param request http 请求
     * @return PageJsonDTO 页面JSON传输对象
     */
    @LoginAuthorization @AccountActivation
    @RequestMapping(value = "/update-profile", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public PageJsonDTO updateProfile(@RequestBody Map<String, Object> requestBodyParamsMap,
                                     HttpServletRequest request) {
        Integer newSex = (Integer) requestBodyParamsMap.get(ParamConst.SEX);
        String newBirthday = (String) requestBodyParamsMap.get(ParamConst.BIRTHDAY);
        String newPosition = (String) requestBodyParamsMap.get(ParamConst.POSITION);
        String newDescription = (String) requestBodyParamsMap.get(ParamConst.DESCRIPTION);

        paramCheckService.checkInstructionOfSpecifyArray(String.valueOf(newSex), "0", "1");
        paramCheckService.paramsNotNull(newBirthday, newPosition, newDescription);
        paramCheckService.check(ParamConst.BIRTHDAY, newBirthday);
        paramCheckService.check(ParamConst.POSITION, newPosition);
        paramCheckService.check(ParamConst.DESCRIPTION, newDescription);

        //get user information in client cookie
        UserDO user = secretService.jwtVerifyTokenByTokenByKey(
               httpService.getAuthenticationCookieValue(request), SecretInfo.JWT_TOKEN_LOGIN_SECRET_KEY
        );

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS,
                userService.alterUserProfile(user.getName(), newSex, newBirthday, newPosition, newDescription));
    }

    /**
     * 修改密码
     *
     * @param requestBodyParamsMap request-body内JSON数据
     * @param request http请求
     * @return PageJsonDTO 页面JSON传输对象
     */
    @LoginAuthorization @AccountActivation
    @RequestMapping(value = "/update-password", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public PageJsonDTO updatePassword(@RequestBody Map<String, Object> requestBodyParamsMap,
                                      HttpServletRequest request) {
        String username = (String) requestBodyParamsMap.get(ParamConst.USERNAME);
        String newPassword = (String) requestBodyParamsMap.get(ParamConst.PASSWORD);

        paramCheckService.check(ParamConst.USERNAME, username);
        paramCheckService.check(ParamConst.PASSWORD, newPassword);

        //confirm input username match logged in user
        UserDO cookieUser = secretService.jwtVerifyTokenByTokenByKey(
                httpService.getAuthenticationCookieValue(request), SecretInfo.JWT_TOKEN_LOGIN_SECRET_KEY
        );
        userService.confirmUserMatchCookieUser(username, cookieUser);

        userService.alterUserPasswordByName(username, newPassword);

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS);
    }

    /**
     * 修改邮箱
     *
     * @param requestBodyParamsMap request-body内JSON数据
     * @param request http请求
     * @return PageJsonDTO 页面JSON传输对象
     */
    @LoginAuthorization
    @RequestMapping(value = "/update-email", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public PageJsonDTO updateEmail(@RequestBody Map<String, Object> requestBodyParamsMap,
                                       HttpServletRequest request) {
        String username = (String) requestBodyParamsMap.get(ParamConst.USERNAME);
        String newEmail = (String) requestBodyParamsMap.get(ParamConst.EMAIL);

        paramCheckService.check(ParamConst.USERNAME, username);
        paramCheckService.check(ParamConst.EMAIL, newEmail);

        UserDO cookieUser = secretService.jwtVerifyTokenByTokenByKey(
                httpService.getAuthenticationCookieValue(request), SecretInfo.JWT_TOKEN_LOGIN_SECRET_KEY
        );
        userService.confirmUserMatchCookieUser(username, cookieUser);

        userService.alterUserEmail(username, newEmail);

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS);
    }

    /**
     * 账户激活（发送激活 email）
     *
     * @param requestBodyParamsMap request-body内JSON数据
     * @return PageJsonDTO 页面JSON传输对象
     */
    @RequestMapping(value = "/activate", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public PageJsonDTO activate(@RequestBody Map<String, Object> requestBodyParamsMap) {
        String email = (String) requestBodyParamsMap.get(ParamConst.EMAIL);

        paramCheckService.check(ParamConst.EMAIL, email);

        userService.confirmUserActivatedByEmail(email);

        //60s send email interval
        long remainAllowSendEmailInterval = redisService.getExpireTime(email);
        if (remainAllowSendEmailInterval != SetConst.REDIS_EXPIRED) {
            return new PageJsonDTO(AjaxRequestStatus.FAIL, ApiMessage.WATI_TIMER,
                    SetConst.EMAIL_TIMER, remainAllowSendEmailInterval / SetConst.THOUSAND);
        }

        //start another thread to send mail
        String token = secretService.getEmailActivateToken(email);

        //get email content(if param activateUrl = null, default use neubbs.properties settings)
        String emailContent = emailService.getEmailContentxtToActivateUserMailHtml(null, token);
        emailService.sendEmail(SetConst.EMAIL_SENDER_NAME, email, SetConst.EMAIL_SUBJECT_ACTIVATE, emailContent);

        //set user send email 60s interval
        redisService.save(email, SetConst.KEY_ACTIVATE, SetConst.EXPIRE_TIME_MS_SIXTY_SECOND);

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS);
    }


    /**
     * 激活账户（ 验证 token ）
     *      - 数据库激活用户
     *      - 修改客户端 cookie (重新保存用户信息)
     *
     * @param token 传入的 token
     * @return PageJsonDTO 页面JSON传输对象
     */
    @RequestMapping(value = "/validate", method = RequestMethod.GET)
    @ResponseBody
    public PageJsonDTO validate(@RequestParam(value = "token", required = false) String token,
                                HttpServletResponse response) {
        paramCheckService.check(ParamConst.TOKEN, token);

        UserDO activatedUser = userService.alterUserActivateStateByToken(token);

        String authentication = secretService.jwtCreateTokenByUser(activatedUser);
        httpService.saveAuthenticationCookie(response, authentication);

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS);
    }

    /**
     * 图片验证码（页面生成图片）
     *
     * @param request http请求
     * @param response http响应
     */
    @RequestMapping(value = "/captcha", method = RequestMethod.GET)
    public void getCaptchaPicture(HttpServletRequest request, HttpServletResponse response) {
        //set response headers
        httpService.setPageResponseHearderToImageType(response);

        //generate captcha text
        String captchaText = captchaService.getCaptchaText();

        //set session attribute
       httpService.setSessionToSaveCaptchaText(request, captchaText);

        //generate captcha image, input captcha text
        BufferedImage outputImage = captchaService.getCaptchaImage(captchaText);

        //page output jpg format image
        httpService.outputPageImageToJPGFormat(response, outputImage);
    }

    /**
     * 检查验证码（比较用户输入是否与图片一致）
     *
     * @param captcha 用户输入验证码
     * @param request http请求
     * @return PageJsonDTO 页面JSON字符串
     */
    @RequestMapping(value = "/check-captcha", method = RequestMethod.GET)
    @ResponseBody
    public PageJsonDTO checkCaptcha(@RequestParam(value = "captcha", required = false)String captcha,
                                        HttpServletRequest request) {
        paramCheckService.check(ParamConst.CAPTCHA, captcha);

        //get session captcha
        String sessionCaptcha = httpService.getSessionCaptchaText(request);

        //compare user input captcha match session captcha
        captchaService.judgeInputCaptchaWhetherSessionCaptcha(captcha, sessionCaptcha);

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS);
    }

    /**
     * 忘记密码（发送临时密码到指定 email）
     *
     *
     * @param requestBodyParamsMap request-body内JSON数据
     * @return PageJsonDTO 页面JSON传输对象
     */
    @RequestMapping(value = "/forget-password", method = RequestMethod.POST)
    @ResponseBody
    public PageJsonDTO sendTemporaryPasswordEmail(@RequestBody Map<String, Object> requestBodyParamsMap) {
        String email = (String) requestBodyParamsMap.get(ParamConst.EMAIL);

        paramCheckService.check(ParamConst.EMAIL, email);

        String randomPassword = randomService.generateSixDigitsRandomPassword();
        userService.alterUserPasswordByEmail(email, randomPassword);

        String emailContent = emailService.getEmailContentToWarnUserGeneratedRandomPassword(email, randomPassword);
        emailService.sendEmail(
                SetConst.EMAIL_SENDER_NAME, email,
                SetConst.EMAIL_SUBJECT_TEMPORARY_PASSWORD, emailContent
        );

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS);
    }

    /**
     * 关注用户
     *
     * @param requestBodyParams request-body内JSON数据
     * @param request http请求
     * @return PageJsonDTO 页面JSON传输对象
     */
    @RequestMapping(value = "/following", method = RequestMethod.POST, consumes = "application/json")
    @LoginAuthorization @AccountActivation
    @ResponseBody
    public PageJsonDTO following(@RequestBody Map<String, Object> requestBodyParams, HttpServletRequest request) {
        Integer followingUserId = (Integer) requestBodyParams.get(ParamConst.USER_ID);

        paramCheckService.check(ParamConst.USER_ID, String.valueOf(followingUserId));

        UserDO cookieUser = secretService.jwtVerifyTokenByTokenByKey(
                httpService.getAuthenticationCookieValue(request), SecretInfo.JWT_TOKEN_LOGIN_SECRET_KEY
        );

        return new PageJsonDTO(AjaxRequestStatus.SUCCESS,
                ParamConst.FOLLOWING_USER_ID, userService.operateFollowUser(cookieUser.getId(), followingUserId));
    }
}
