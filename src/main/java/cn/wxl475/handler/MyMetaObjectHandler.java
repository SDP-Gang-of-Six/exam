package cn.wxl475.handler;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    //插入时的填充策略
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("start insert fill createTime");
        //strictInsertFill(MetaObject metaObject, String fieldName, Class<T> fieldType, E fieldVal)
        this.strictInsertFill(metaObject,"createTime", String.class, DateUtil.now());// 起始版本 3.3.0(推荐使用)
        log.info("start insert fill updateTime");
        this.strictInsertFill(metaObject, "updateTime", String.class, DateUtil.now()); // 起始版本 3.3.0(推荐)
    }

    //更新时的填充策略
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("start update fill updateTime");
        this.strictUpdateFill(metaObject, "updateTime", String.class, DateUtil.now()); // 起始版本 3.3.0(推荐)
    }
}
