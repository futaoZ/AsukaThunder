package com.ford.asukathunder.common.entity.jpa.other;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @ClassName: Author
 * @author: Ford.Zhang
 * @version: 1.0
 * 2020/3/18 下午 2:23
 **/
@Entity
@Data
@Table(name = "author")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "姓名不能为空")
    @Size(min=2, max=20)
    @Column(nullable = false, length = 20)
    private String name;

    @OneToMany(mappedBy = "author",cascade=CascadeType.ALL,fetch= FetchType.LAZY)
    //级联保存、更新、删除、刷新;延迟加载。当删除用户，会级联删除该用户的所有文章
    //拥有mappedBy注解的实体类为关系被维护端
    //mappedBy=”author”中的author是Article中的author属性
    private List<Article> articleList;
}
