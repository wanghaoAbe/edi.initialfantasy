package org.edi.initialfantasy.service;

import org.edi.freamwork.cryptogram.MD5Util;
import org.edi.initialfantasy.bo.company.Company;
import org.edi.initialfantasy.bo.user.User;
import org.edi.initialfantasy.bo.userauthrization.UserAuth;
import org.edi.initialfantasy.data.DataConvert;
import org.edi.initialfantasy.dto.*;
import org.edi.initialfantasy.repository.CompanyMapper;
import org.edi.initialfantasy.repository.UserAuthMapper;
import org.edi.initialfantasy.repository.UserMapper;
import org.edi.initialfantasy.util.UUIDUtil;
import org.glassfish.jersey.server.JSONP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fancy
 * @date 2018/5/25
 */
@Path("/v1")
@Transactional
public class UserService implements IUserService{
    @Autowired
    private UserMapper userDao;
    @Autowired
    private UserAuthMapper userAuthDao;
    @Autowired
    private CompanyMapper companyDao;


    @POST
    @Override
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/userauthrization")
    //用户登录
    public IResult<IUserAuthrizationResult> Login(Userauthrization userauthrization) {
        System.out.println(userauthrization);
        Result rs = new Result();
        UserAuthrizationResult uaResult = new UserAuthrizationResult();
        List<UserAuthrizationResult> listResult = new ArrayList<UserAuthrizationResult>();
        try {
            Company company = companyDao.serchCompanyId(userauthrization.getCompanyName());
            User loginUser =  userDao.getUserByCompanyId(userauthrization.getUserName(),company.getCompanyId());
            String hmacPassword = MD5Util.byteArrayToHexString(MD5Util.encryptHMAC(loginUser.getMobilePassword().getBytes(),"avatech"));
            if (hmacPassword.equals(userauthrization.getPassword())) {
                long NextDayTimeMillis = Long.parseLong(DataConvert.dateToStamp());
                UserAuth userRecord = userAuthDao.serchLoginRecord(loginUser.getUserName());
                if(userRecord==null) {
                    String authToken = UUIDUtil.randomUUID32();
                    userRecord = new UserAuth(loginUser.getUserName(), loginUser.getIsMobileUser(), "客户", authToken, NextDayTimeMillis, "Y");
                    userAuthDao.saveLoginRecord(userRecord);
                    uaResult = new UserAuthrizationResult(authToken,NextDayTimeMillis);
                }else{
                    Long currentTimeMillis = System.currentTimeMillis();
                    if(currentTimeMillis<userRecord.getAuthExpires()){
                        uaResult = new UserAuthrizationResult(userRecord.getAuthToken(),userRecord.getAuthExpires());
                    }else{
                        UserAuth userAuth = new UserAuth(userRecord.getUserId(),NextDayTimeMillis);
                        userAuthDao.updateAuthExpires(userAuth);
                        uaResult = new UserAuthrizationResult(userRecord.getAuthToken(),NextDayTimeMillis);
                    }
                    UserAuth uauth = new UserAuth(userRecord.getUserId(),"Y");
                    userAuthDao.updateActive(uauth);
                }
                listResult.add(uaResult);
                rs = new Result("0", "ok", listResult);
            } else {
                rs = new Result("1", "fail", listResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
            rs = new Result("1", "fail", listResult);
        }
        return rs;

    }

    @GET
    @Override
    @JSONP(queryParam="callback")
    @Produces("application/x-javascript")
    @Path("/userauthrization")
    public IResult<IUserAuthrizationResult> LoginUser(@QueryParam("companyName")String companyName,@QueryParam("userName")String userName,@QueryParam("password")String password) {
        Result rs = new Result();
        UserAuthrizationResult uaResult = new UserAuthrizationResult();
        List<UserAuthrizationResult> listResult = new ArrayList<UserAuthrizationResult>();
        try {
            //根据公司名称和用户名查询用户信息，并且为密码参数进行MD5加密与用户密码进行比对
            Company company = companyDao.serchCompanyId(companyName);
            User loginUser =  userDao.getUserByCompanyId(userName,company.getCompanyId());
            String hmacPassword = MD5Util.byteArrayToHexString(MD5Util.encryptHMAC(loginUser.getMobilePassword().getBytes(),"avatech"));
            if (hmacPassword.equals(password)) {
                //用户密码正确，获取截止到登录日期后一天的13位时间戳作为有效期
                long NextDayTimeMillis = Long.parseLong(DataConvert.dateToStamp());
                //查询用户历史登录记录
                UserAuth userRecord = userAuthDao.serchLoginRecord(loginUser.getUserName());
                if(userRecord==null) {
                    //没有用户记录则新建
                    String authToken = UUIDUtil.randomUUID32();
                    userRecord = new UserAuth(loginUser.getUserName(), loginUser.getIsMobileUser(), "客户", authToken, NextDayTimeMillis, "Y");
                    userAuthDao.saveLoginRecord(userRecord);
                    uaResult = new UserAuthrizationResult(authToken,NextDayTimeMillis);
                }else{
                    //存在用户记录则得到当前登录时间的时间戳，和记录时间戳进行比对，在有效期内则返回，否则更新
                    Long currentTimeMillis = System.currentTimeMillis();
                    if(currentTimeMillis<userRecord.getAuthExpires()){
                        uaResult = new UserAuthrizationResult(userRecord.getAuthToken(),userRecord.getAuthExpires());
                    }else{
                        UserAuth userAuth = new UserAuth(userRecord.getUserId(),NextDayTimeMillis);
                        userAuthDao.updateAuthExpires(userAuth);
                        uaResult = new UserAuthrizationResult(userRecord.getAuthToken(),NextDayTimeMillis);
                    }
                    UserAuth uauth = new UserAuth(userRecord.getUserId(),"Y");
                    userAuthDao.updateActive(uauth);
                }
                listResult.add(uaResult);
                rs = new Result("0", "ok", listResult);
            } else {
                rs = new Result("1", "fail", listResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
            rs = new Result("1", "fail", listResult);
        }
        return rs;

    }



    @DELETE
    @Override
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/userauthrization")
    //用户退出
    public IResult Logout(@QueryParam("token")String token) {
        Result rs = new Result();
        if(token==null||token.equals("")){
            rs = new Result("1","请用您的token来退出!",null);
        }else {
            UserAuth auth = userAuthDao.serchAuthByToken(token);
            if (auth == null) {
                rs = new Result("1", "您的token不存在!", null);
            } else {
                auth.setIsActive("N");
                userAuthDao.updateActive(auth);
                rs = new Result("0", "ok", null);
            }
        }
        return rs;
    }


   /* @DELETE
    @Override
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/userauthrization")
    //用户退出
    public IResult Logout(Token token) {
        Result rs = new Result();
        if(token.getToken()==null){
            rs = new Result("1","请用您的token来退出!",null);
        }else {
            UserAuth auth = userAuthDao.serchAuthByToken(token.getToken());
            if (auth == null) {
                rs = new Result("1", "您的token不存在!", null);
            } else {
                auth.setIsActive("N");
                userAuthDao.updateActive(auth);
                rs = new Result("0", "ok", null);
            }
        }
        return rs;
    }*/




    @GET
    @Path("/getname")
    @Produces("text/plain")
    public String UserLogin(){
        return "hello";
    }
}
