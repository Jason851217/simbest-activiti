package com.simbest.activiti.query.service.impl;

import com.simbest.activiti.query.mapper.ActTaskAssigneMapper;
import com.simbest.activiti.query.model.ActTaskAssigne;
import com.simbest.activiti.query.service.IActTaskAssigneService;
import com.simbest.cores.service.impl.GenericMapperService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActTaskAssigneService extends GenericMapperService<ActTaskAssigne, Long> implements IActTaskAssigneService {
    public final static Log log = LogFactory.getLog(ActTaskAssigneService.class);

    private ActTaskAssigneMapper mapper;

    public ActTaskAssigneService(SqlSession sqlSession, Class<ActTaskAssigne> persistentMapper) {
        super(sqlSession, persistentMapper);
    }

    @Autowired
    public ActTaskAssigneService(@Qualifier(value = "sqlSessionTemplateSimple") SqlSession sqlSession) {
        super(sqlSession);
        this.mapper = sqlSession.getMapper(ActTaskAssigneMapper.class);
        super.setMapper(mapper);
    }

    /**
     * 查询任务办理人和候选人
     * @param taskId
     * @return
     */
    @Override
    public List<String> queryToDoUser(String taskId) {
        return result;
    }

    /**
     * 查询任务办理人
     * @param taskId
     * @return
     */
    @Override
        return mapper.queryAssignee(taskId);
    }

    /**
     * 查询任务候选人
     * @param taskId
     * @return
     */
    @Override
    public List<String> queryCandidate(String taskId) {
        return mapper.queryCandidate(taskId);
    }
}