package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.model.dto.AccountReqDTO;
import com.feijimiao.xianyuassistant.model.dto.AddAccountRespDTO;
import com.feijimiao.xianyuassistant.model.dto.DeleteAccountReqDTO;
import com.feijimiao.xianyuassistant.model.dto.DeleteAccountRespDTO;
import com.feijimiao.xianyuassistant.model.dto.GetAccountDetailReqDTO;
import com.feijimiao.xianyuassistant.model.dto.GetAccountDetailRespDTO;
import com.feijimiao.xianyuassistant.model.dto.GetAccountListRespDTO;
import com.feijimiao.xianyuassistant.model.dto.UpdateAccountReqDTO;
import com.feijimiao.xianyuassistant.model.dto.UpdateAccountRespDTO;
import com.feijimiao.xianyuassistant.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账号管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {

    @Autowired
    private XianyuAccountMapper accountMapper;
    
    @Autowired
    private AccountService accountService;

    /**
     * 获取账号列表
     */
    @PostMapping("/list")
    public ResultObject<GetAccountListRespDTO> getAccountList() {
        try {
            List<XianyuAccount> accounts = accountMapper.selectList(null);
            GetAccountListRespDTO respDTO = new GetAccountListRespDTO();
            respDTO.setAccounts(accounts);
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取账号列表失败", e);
            return ResultObject.failed("获取账号列表失败: " + e.getMessage());
        }
    }

    /**
     * 添加账号
     */
    @PostMapping("/add")
    public ResultObject<AddAccountRespDTO> addAccount(@RequestBody AccountReqDTO reqDTO) {
        try {
            log.info("添加账号请求: accountNote={}", reqDTO.getAccountNote());
            
            if (reqDTO.getCookie() == null || reqDTO.getCookie().isEmpty()) {
                return ResultObject.failed("Cookie不能为空");
            }
            
            Long accountId = accountService.saveAccountAndCookie(
                    reqDTO.getAccountNote(),
                    reqDTO.getUnb(),
                    reqDTO.getCookie()
            );
            
            AddAccountRespDTO respDTO = new AddAccountRespDTO();
            respDTO.setAccountId(accountId);
            respDTO.setMessage("添加成功");
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("添加账号失败", e);
            return ResultObject.failed("添加账号失败: " + e.getMessage());
        }
    }

    /**
     * 更新账号
     */
    @PostMapping("/update")
    public ResultObject<UpdateAccountRespDTO> updateAccount(@RequestBody UpdateAccountReqDTO reqDTO) {
        try {
            log.info("更新账号请求: accountId={}", reqDTO.getAccountId());
            
            if (reqDTO.getAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            
            XianyuAccount account = accountMapper.selectById(reqDTO.getAccountId());
            if (account == null) {
                return ResultObject.failed("账号不存在");
            }
            
            // 只更新账号备注
            if (reqDTO.getAccountNote() != null) {
                account.setAccountNote(reqDTO.getAccountNote());
            }
            
            accountMapper.updateById(account);
            
            // 不再更新Cookie和UNB
            
            UpdateAccountRespDTO respDTO = new UpdateAccountRespDTO();
            respDTO.setMessage("更新成功");
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("更新账号失败", e);
            return ResultObject.failed("更新账号失败: " + e.getMessage());
        }
    }

    /**
     * 删除账号
     */
    @PostMapping("/delete")
    public ResultObject<DeleteAccountRespDTO> deleteAccount(@RequestBody DeleteAccountReqDTO reqDTO) {
        try {
            Long id = reqDTO.getAccountId();
            log.info("删除账号请求: accountId={}", id);
            
            XianyuAccount account = accountMapper.selectById(id);
            if (account == null) {
                return ResultObject.failed("账号不存在");
            }
            
            // 删除账号关联的所有数据
            accountService.deleteAccountAndRelatedData(id);
            
            DeleteAccountRespDTO respDTO = new DeleteAccountRespDTO();
            respDTO.setMessage("删除成功");
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("删除账号失败", e);
            return ResultObject.failed("删除账号失败: " + e.getMessage());
        }
    }

    /**
     * 获取账号详情
     */
    @PostMapping("/detail")
    public ResultObject<GetAccountDetailRespDTO> getAccountDetail(@RequestBody GetAccountDetailReqDTO reqDTO) {
        try {
            Long id = reqDTO.getAccountId();
            XianyuAccount account = accountMapper.selectById(id);
            if (account == null) {
                return ResultObject.failed("账号不存在");
            }
            GetAccountDetailRespDTO respDTO = new GetAccountDetailRespDTO();
            respDTO.setAccount(account);
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取账号详情失败", e);
            return ResultObject.failed("获取账号详情失败: " + e.getMessage());
        }
    }


}
