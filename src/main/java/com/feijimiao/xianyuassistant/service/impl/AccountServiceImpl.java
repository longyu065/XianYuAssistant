package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.entity.XianyuCookie;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper;
import com.feijimiao.xianyuassistant.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 账号服务实现类
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private XianyuAccountMapper accountMapper;

    @Autowired
    private XianyuCookieMapper cookieMapper;
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 获取当前时间字符串
     */
    private String getCurrentTimeString() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }
    
    /**
     * 获取未来时间字符串
     */
    private String getFutureTimeString(int days) {
        return LocalDateTime.now().plusDays(days).format(DATETIME_FORMATTER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAccountAndCookie(String accountNote, String unb, String cookieText) {
        return saveAccountAndCookie(accountNote, unb, cookieText, null);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAccountAndCookie(String accountNote, String unb, String cookieText, String mH5Tk) {
        try {
            log.info("开始保存账号和Cookie: accountNote={}, unb={}, 包含m_h5_tk={}", 
                    accountNote, unb, mH5Tk != null);

            // 1. 检查账号是否已存在（根据UNB）
            LambdaQueryWrapper<XianyuAccount> accountQuery = new LambdaQueryWrapper<>();
            accountQuery.eq(XianyuAccount::getUnb, unb);
            XianyuAccount existingAccount = accountMapper.selectOne(accountQuery);

            Long accountId;
            if (existingAccount != null) {
                // 账号已存在，更新信息
                accountId = existingAccount.getId();
                existingAccount.setAccountNote(accountNote);
                existingAccount.setStatus(1); // 正常状态
                existingAccount.setUpdatedTime(getCurrentTimeString());
                accountMapper.updateById(existingAccount);
                log.info("账号已存在，更新账号信息: accountId={}", accountId);
            } else {
                // 创建新账号
                XianyuAccount account = new XianyuAccount();
                account.setAccountNote(accountNote);
                account.setUnb(unb);
                account.setStatus(1);
                account.setCreatedTime(getCurrentTimeString());
                account.setUpdatedTime(getCurrentTimeString());
                accountMapper.insert(account);
                accountId = account.getId();
                log.info("创建新账号成功: accountId={}", accountId);
            }

            // 2. 保存或更新Cookie
            LambdaQueryWrapper<XianyuCookie> cookieQuery = new LambdaQueryWrapper<>();
            cookieQuery.eq(XianyuCookie::getXianyuAccountId, accountId);
            XianyuCookie existingCookie = cookieMapper.selectOne(cookieQuery);

            if (existingCookie != null) {
                // Cookie已存在，更新
                existingCookie.setCookieText(cookieText);
                existingCookie.setMH5Tk(mH5Tk);
                existingCookie.setCookieStatus(1); // 有效状态
                existingCookie.setExpireTime(getFutureTimeString(30)); // 30天后过期
                existingCookie.setUpdatedTime(getCurrentTimeString());
                cookieMapper.updateById(existingCookie);
                log.info("更新Cookie成功: cookieId={}, m_h5_tk={}", 
                        existingCookie.getId(), mH5Tk != null ? "已保存" : "未提供");
            } else {
                // 创建新Cookie
                XianyuCookie cookie = new XianyuCookie();
                cookie.setXianyuAccountId(accountId);
                cookie.setCookieText(cookieText);
                cookie.setMH5Tk(mH5Tk);
                cookie.setCookieStatus(1);
                cookie.setExpireTime(getFutureTimeString(30));
                cookie.setCreatedTime(getCurrentTimeString());
                cookie.setUpdatedTime(getCurrentTimeString());
                cookieMapper.insert(cookie);
                log.info("创建新Cookie成功: cookieId={}, m_h5_tk={}", 
                        cookie.getId(), mH5Tk != null ? "已保存" : "未提供");
            }

            log.info("保存账号和Cookie完成: accountId={}, accountNote={}", accountId, accountNote);
            return accountId;

        } catch (Exception e) {
            log.error("保存账号和Cookie失败: accountNote={}, unb={}", accountNote, unb, e);
            throw new RuntimeException("保存账号和Cookie失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getCookieByAccountId(Long accountId) {
        try {
            log.info("根据账号ID获取Cookie: accountId={}", accountId);

            // 查询最新的有效Cookie
            LambdaQueryWrapper<XianyuCookie> cookieQuery = new LambdaQueryWrapper<>();
            cookieQuery.eq(XianyuCookie::getXianyuAccountId, accountId)
                    .eq(XianyuCookie::getCookieStatus, 1) // 只查询有效的Cookie
                    .orderByDesc(XianyuCookie::getCreatedTime)
                    .last("LIMIT 1");
            XianyuCookie cookie = cookieMapper.selectOne(cookieQuery);

            if (cookie == null) {
                log.warn("未找到有效Cookie: accountId={}", accountId);
                return null;
            }

            log.info("获取Cookie成功: accountId={}", accountId);
            return cookie.getCookieText();

        } catch (Exception e) {
            log.error("获取Cookie失败: accountId={}", accountId, e);
            return null;
        }
    }

    @Override
    public String getCookieByUnb(String unb) {
        try {
            // 1. 根据UNB查询账号
            LambdaQueryWrapper<XianyuAccount> accountQuery = new LambdaQueryWrapper<>();
            accountQuery.eq(XianyuAccount::getUnb, unb);
            XianyuAccount account = accountMapper.selectOne(accountQuery);

            if (account == null) {
                log.warn("未找到账号: unb={}", unb);
                return null;
            }

            // 2. 查询Cookie
            LambdaQueryWrapper<XianyuCookie> cookieQuery = new LambdaQueryWrapper<>();
            cookieQuery.eq(XianyuCookie::getXianyuAccountId, account.getId())
                    .eq(XianyuCookie::getCookieStatus, 1) // 只查询有效的Cookie
                    .orderByDesc(XianyuCookie::getCreatedTime)
                    .last("LIMIT 1");
            XianyuCookie cookie = cookieMapper.selectOne(cookieQuery);

            if (cookie == null) {
                log.warn("未找到有效Cookie: accountId={}", account.getId());
                return null;
            }

            log.info("获取Cookie成功: unb={}, accountId={}", unb, account.getId());
            return cookie.getCookieText();

        } catch (Exception e) {
            log.error("获取Cookie失败: unb={}", unb, e);
            return null;
        }
    }

    @Override
    public String getCookieByAccountNote(String accountNote) {
        try {
            // 1. 根据账号备注查询账号
            LambdaQueryWrapper<XianyuAccount> accountQuery = new LambdaQueryWrapper<>();
            accountQuery.eq(XianyuAccount::getAccountNote, accountNote);
            XianyuAccount account = accountMapper.selectOne(accountQuery);

            if (account == null) {
                log.warn("未找到账号: accountNote={}", accountNote);
                return null;
            }

            // 2. 查询Cookie
            LambdaQueryWrapper<XianyuCookie> cookieQuery = new LambdaQueryWrapper<>();
            cookieQuery.eq(XianyuCookie::getXianyuAccountId, account.getId())
                    .eq(XianyuCookie::getCookieStatus, 1)
                    .orderByDesc(XianyuCookie::getCreatedTime)
                    .last("LIMIT 1");
            XianyuCookie cookie = cookieMapper.selectOne(cookieQuery);

            if (cookie == null) {
                log.warn("未找到有效Cookie: accountId={}", account.getId());
                return null;
            }

            log.info("获取Cookie成功: accountNote={}, accountId={}", accountNote, account.getId());
            return cookie.getCookieText();

        } catch (Exception e) {
            log.error("获取Cookie失败: accountNote={}", accountNote, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCookie(Long accountId, String cookieText) {
        try {
            log.info("更新Cookie: accountId={}", accountId);

            // 查询现有Cookie
            LambdaQueryWrapper<XianyuCookie> cookieQuery = new LambdaQueryWrapper<>();
            cookieQuery.eq(XianyuCookie::getXianyuAccountId, accountId);
            XianyuCookie cookie = cookieMapper.selectOne(cookieQuery);

            if (cookie != null) {
                // 更新现有Cookie
                cookie.setCookieText(cookieText);
                cookie.setCookieStatus(1);
                cookie.setExpireTime(getFutureTimeString(30));
                cookie.setUpdatedTime(getCurrentTimeString());
                cookieMapper.updateById(cookie);
            } else {
                // 创建新Cookie
                cookie = new XianyuCookie();
                cookie.setXianyuAccountId(accountId);
                cookie.setCookieText(cookieText);
                cookie.setCookieStatus(1);
                cookie.setExpireTime(getFutureTimeString(30));
                cookie.setCreatedTime(getCurrentTimeString());
                cookie.setUpdatedTime(getCurrentTimeString());
                cookieMapper.insert(cookie);
            }

            log.info("更新Cookie成功: accountId={}", accountId);
            return true;

        } catch (Exception e) {
            log.error("更新Cookie失败: accountId={}", accountId, e);
            return false;
        }
    }
    
    @Override
    public String getMh5tkByAccountId(Long accountId) {
        try {
            log.info("根据账号ID获取m_h5_tk: accountId={}", accountId);

            // 查询Cookie记录
            LambdaQueryWrapper<XianyuCookie> cookieQuery = new LambdaQueryWrapper<>();
            cookieQuery.eq(XianyuCookie::getXianyuAccountId, accountId)
                    .eq(XianyuCookie::getCookieStatus, 1)
                    .orderByDesc(XianyuCookie::getCreatedTime)
                    .last("LIMIT 1");
            XianyuCookie cookie = cookieMapper.selectOne(cookieQuery);

            if (cookie == null) {
                log.warn("未找到Cookie记录: accountId={}", accountId);
                return null;
            }

            String mH5Tk = cookie.getMH5Tk();
            if (mH5Tk != null && !mH5Tk.isEmpty()) {
                log.info("获取m_h5_tk成功: accountId={}", accountId);
            } else {
                log.warn("m_h5_tk为空: accountId={}", accountId);
            }
            
            return mH5Tk;

        } catch (Exception e) {
            log.error("获取m_h5_tk失败: accountId={}", accountId, e);
            return null;
        }
    }
}
