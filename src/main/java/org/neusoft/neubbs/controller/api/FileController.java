package org.neusoft.neubbs.controller.api;

import org.neusoft.neubbs.constant.api.ApiMessage;
import org.neusoft.neubbs.controller.annotation.AccountActivation;
import org.neusoft.neubbs.controller.annotation.LoginAuthorization;
import org.neusoft.neubbs.dto.ApiJsonDTO;
import org.neusoft.neubbs.entity.UserDO;
import org.neusoft.neubbs.service.IFtpService;
import org.neusoft.neubbs.service.IHttpService;
import org.neusoft.neubbs.service.ISecretService;
import org.neusoft.neubbs.service.IUserService;
import org.neusoft.neubbs.service.IValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * 文件 api
 *      + 上传用户头像
 *
 * @author Suvan
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final IValidationService validationService;
    private final IUserService userService;
    private final IHttpService httpService;
    private final ISecretService secretService;
    private final IFtpService ftpService;

    @Autowired
    public FileController(IValidationService validationService, IUserService userService,
                          IHttpService httpService, ISecretService secretService,
                          IFtpService ftpService) {
        this.validationService = validationService;
        this.userService = userService;
        this.httpService = httpService;
        this.secretService = secretService;
        this.ftpService = ftpService;
    }

    /**
     * 上传用户头像
     *      - 上传文件类型需满足 MediaType.MULTIPART_FORM_DATA_VALUE（multipart/form-data）
     *      - 执行流程
     *          - 检查上传头像规范（空，类型，文件大小）
     *          - Cookie 内获取用户信息
     *          - ftp 服务上传用户头像
     *          - 修改数据库用户个人信息（头像名）
     *
     * @param avatarFile 用户上传的文件对象
     * @return ApiJsonDTO 接口 JSON 传输对象
     */
    @LoginAuthorization @AccountActivation
    @RequestMapping(value = "/avatar", method = RequestMethod.POST, consumes = "multipart/form-data")
    public ApiJsonDTO uploadUserAvatar(@RequestParam("avatarImageFile")MultipartFile avatarFile) {
        validationService.checkUserUploadAvatarNorm(avatarFile);

        UserDO cookieUser = secretService.getUserInfoByAuthentication(httpService.getAuthenticationCookieValue());
        ftpService.uploadUserAvatar(cookieUser, avatarFile);

        userService.alterUserAvatar(cookieUser.getName(), ftpService.generateServerAvatarFileName(avatarFile));

        return new ApiJsonDTO().success().message(ApiMessage.UPLOAD_SUCCESS);
    }
}
