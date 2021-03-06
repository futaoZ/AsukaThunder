package com.ford.asukathunder.common.entity.jpa.city_country;

import com.ford.asukathunder.common.entity.base.BaseEntity;
import com.ford.asukathunder.common.util.SnowflakeIdWorker;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @ClassName: CityEntity
 * @author: Ford.Zhang
 * @version: 1.0
 * 2020/3/20 上午 11:46
 **/
@Entity
@Table(name = "city")
@Getter
@Setter
public class CityEntity extends BaseEntity {
    @Override
    public void setEntityId(SnowflakeIdWorker snowflakeIdWorker) {
        if (StringUtils.isBlank(id)) {
            id = String.valueOf(snowflakeIdWorker.nextId());
        }
    }
    @Id
    @Column(name = "user_id", length = 32, updatable = false, unique = true)
    private String id;
    /**
     * 城市名称
     */
    @Column(name = "name", length = 50)
    private String cityName;
    /**
     * 人口
     */
    @Column(name = "population")
    private Long population;

}
