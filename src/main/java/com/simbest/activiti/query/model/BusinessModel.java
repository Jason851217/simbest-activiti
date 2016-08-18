/**
 * 版权所有 © 北京晟壁科技有限公司 2008-2016。保留一切权利!
 */
package com.simbest.activiti.query.model;

import com.simbest.cores.model.LogicModel;
import com.simbest.cores.utils.annotations.ProcessProperty;

import javax.persistence.*;

@MappedSuperclass
public class BusinessModel<T> extends LogicModel<T> {

    private static final long serialVersionUID = -8887330587069785067L;

    //所有业务单据的主键必须统一类型，否则无法写入待办和审批记录
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Column(columnDefinition = "TINYINT default 0")
    protected Boolean iscg;

    protected String code;

    protected String title;

    @Transient
    private Integer createOrgId;

    @Transient
    private String createOrgName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getIscg() {
        return iscg;
    }

    public void setIscg(Boolean iscg) {
        this.iscg = iscg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCreateOrgId() {
        return createOrgId;
    }

    public void setCreateOrgId(Integer createOrgId) {
        this.createOrgId = createOrgId;
    }

    public String getCreateOrgName() {
        return createOrgName;
    }

    public void setCreateOrgName(String createOrgName) {
        this.createOrgName = createOrgName;
    }
}
