package com.example.escort.module.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 业务通知发布器
 * 通知属于附属动作：主事务提交成功后再发送，避免通知失败影响订单/审核主流程；
 * 发送异常记录 error 日志，不做静默吞掉。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final NotifyService notifyService;

    public void publishAfterCommit(Long recipientId, String title, String content,
                                   String bizType, Long bizId) {
        if (recipientId == null) return;
        Runnable task = () -> {
            try {
                notifyService.send(recipientId, title, content, bizType, bizId);
            } catch (Exception e) {
                log.error("通知发送失败 recipientId={}, title={}", recipientId, title, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
