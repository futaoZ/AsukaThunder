package com.ford.asukathunder.service.impl;

import com.ford.asukathunder.common.config.PageResult;
import com.ford.asukathunder.common.entity.role.UserRoleRef;
import com.ford.asukathunder.common.entity.user.User;
import com.ford.asukathunder.common.util.BeanCopierUtil;
import com.ford.asukathunder.common.util.UserUtils;
import com.ford.asukathunder.controller.user.dto.PageUserDTO;
import com.ford.asukathunder.repository.UserRepository;
import com.ford.asukathunder.service.PermissionService;
import com.ford.asukathunder.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: UserServiceImpl
 * @author: Ford.Zhang
 * @version: 1.0
 * 2019/12/27 下午 2:26
 **/
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserRepository userRepository;
    @Resource
    private PermissionService permissionService;
    @PersistenceContext
    EntityManager entityManager;

    @Override
    public User queryByAccountAndPassword(String account, String password) {
        return userRepository.findByAccountAndPasswordAndIsDeleteFalseAndIsUseTrue(account,password);
    }

    @Override
    public User queryByAccount(String account) {
        return userRepository.findByAccount(account);
    }

    @Override
    public User queryById(String userId) {
        return userRepository.findByUserIdAndIsDeleteFalseAndIsUseTrue(userId);
    }

    @Override
    public Page<User> queryUsers(String nickName, String account, String roleId, Pageable pageable) {
        Specification<User> spec = (root, criteriaQuery, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (StringUtils.isNotEmpty(nickName)) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(root.get("nickname").as(String.class), "%" + nickName + "%"));
            }
            if (StringUtils.isNotEmpty(account)) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(root.get("account").as(String.class), "%" + account + "%"));
            }
            if (StringUtils.isNotEmpty(roleId)) {

                Path<UserRoleRef> userRolePath = root.join("userRoleRef", JoinType.LEFT);
                Subquery<String> subQuery = criteriaQuery.subquery(String.class);
                Root<UserRoleRef> userRoleRoot = subQuery.from(UserRoleRef.class);
                subQuery.select(userRoleRoot.get("userRoleId"));
                Predicate subPredicate = criteriaBuilder.conjunction();
                subPredicate = criteriaBuilder.and(subPredicate, criteriaBuilder.equal(userRoleRoot.get("role").get("roleId")
                        .as(String.class), roleId));
                subQuery.where(subPredicate);
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.in(userRolePath.get("userRoleId")).value(subQuery));

            }
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("isDelete").as(Boolean.class), false));
            criteriaQuery.distinct(true).where(predicate);
            return criteriaQuery.getRestriction();
        };

        return userRepository.findAll(spec, pageable);
    }

    @Override
    public PageResult<PageUserDTO> query(String nickName, String account, String roleId, Integer page, Integer size) {
        //新建排序、分页
        Pageable  pageable = PageRequest.of(page - 1, size, Sort.Direction.DESC, "createTime");
        //实现接口方法specification，添加条件
        Specification<User> spec = (root, criteriaQuery, criteriaBuilder) -> {
            //混合条件查询
            Predicate predicate = criteriaBuilder.conjunction();
            if (StringUtils.isNotEmpty(nickName)) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(root.get("nickname").as(String.class), "%" + nickName + "%"));
            }
            if (StringUtils.isNotEmpty(account)) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(root.get("account").as(String.class), "%" + account + "%"));
            }
            if (StringUtils.isNotEmpty(roleId)) {
                //多表关联查询
                Path<UserRoleRef> userRolePath = root.join("userRoleRef", JoinType.LEFT);
                //子查询
                Subquery<String> subQuery = criteriaQuery.subquery(String.class);
                Root<UserRoleRef> userRoleRoot = subQuery.from(UserRoleRef.class);
                subQuery.select(userRoleRoot.get("userRoleId"));
                Predicate subPredicate = criteriaBuilder.conjunction();
                //拼接查询条件
                subPredicate = criteriaBuilder.and(subPredicate, criteriaBuilder.equal(userRoleRoot.get("role").get("roleId")
                        .as(String.class), roleId));
                subQuery.where(subPredicate);
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.in(userRolePath.get("userRoleId")).value(subQuery));
            }
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("isDelete").as(Boolean.class), false));
            criteriaQuery.distinct(true).where(predicate);
            return criteriaQuery.getRestriction();
        };
        //注意：
        //如果实体类user和DTO字段名不对应，将不会把值正确的赋予，需手动再赋值
        Page<User> pages = userRepository.findAll(spec, pageable);
        List<User> userList = pages.getContent();
        List<PageUserDTO> userDTOList = BeanCopierUtil.cloneObject(userList, PageUserDTO.class);
        // 科室名称
        userDTOList.stream()
                .forEach(userDTO -> userList.stream()
                        // 主键相等
                        .filter(user -> userDTO.getUserId().equals(user.getUserId()))
                        .forEach(user -> {
                            //昵称
                            userDTO.setNickName(user.getNickname());
                            // 角色
                            userDTO.setRoles(user.getUserRoleRef()
                                    .stream()
                                    .filter(ref -> ref.getRole().getEnable())
                                    .map(ref -> ref.getRole().getRoleName())
                                    .collect(Collectors.toList()));
                        })
                );
        return new PageResult<>(pages.getTotalElements(), userDTOList);
    }

    @Override
    public Boolean isAccountDuplicate(String account) {
        User user = userRepository.findByAccount(account);
        return null != user;
    }

    @Override
    public Boolean isPhoneDuplicate(String phone) {
        User user = userRepository.findByMobilePhone(phone);
        return null != user;
    }

    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    public void update(User dbUser, User inputUser) {
        dbUser.setNickname(inputUser.getNickname());
        dbUser.setAvatar(inputUser.getAvatar());
        dbUser.setBirthday(inputUser.getBirthday());
        dbUser.setAge(inputUser.getAge());
        dbUser.setConstellation(inputUser.getConstellation());
        dbUser.setRealName(inputUser.getRealName());
        dbUser.setEmail(inputUser.getEmail());
        dbUser.setGender(inputUser.getGender());
        dbUser.setIsUse(inputUser.getIsUse());
        dbUser.setMobilePhone(inputUser.getMobilePhone());

        // 处理用户角色关系
        List<UserRoleRef> refs = dbUser.getUserRoleRef();
        refs.clear();
        if (!CollectionUtils.isEmpty(inputUser.getUserRoleRef())) {
            refs.addAll(inputUser.getUserRoleRef());
        }
        userRepository.save(dbUser);
    }

    @Override
    public Boolean isAccountDuplicate(String account, String userId) {
        User user = userRepository.findByAccountAndUserIdNot(account, userId);
        return null != user;
    }

    @Override
    public Boolean isPhoneDuplicate(String phone, String userId) {
        User user = userRepository.findByMobilePhoneAndUserIdNot(phone,userId);
        return null != user;
    }

    @Override
    public User getValidUser(String userId) {
        return userRepository.findByUserIdAndIsDeleteFalseAndIsUseTrue(userId);
    }

    @Override
    public Boolean getCanEdit(User targetUser) {
        User currentUser = this.queryById(UserUtils.getUserId());
        //允许拥有USER_UPDATE权限的人可以添加用户操作
        Boolean isAdmin = permissionService.isPermission(currentUser, "USER_UPDATE");
        // 当前登录用户是超级管理员，可以修改任何信息，但是不能修改自己的角色和启用状态
        if (currentUser.isRoot() || isAdmin) {
            return true;
        }
        // 当前登录用户是管理员
        if (currentUser.isAdmin()) {
            return !(targetUser.isAdmin() || targetUser.isRoot());
        }
        // 当前登录用户是其他角色，都无法修改
        return false;
    }

    @Override
    public Boolean getCanDelete(User targetUser) {
        User currentUser = this.queryById(UserUtils.getUserId());
        // 当前登录用户是超级管理员，可以删除除自己外的用户
        //允许拥有USER_UPDATE权限的人可以添加用户操作
        Boolean isAdmin = permissionService.isPermission(currentUser, "USER_UPDATE");
        if (currentUser.isRoot() || isAdmin) {
            return !targetUser.isRoot();
        }
        // 当前登录用户是管理员
        if (currentUser.isAdmin()) {
            return !(targetUser.isAdmin() || targetUser.isRoot());
        }
        // 当前登录用户是其他角色，都无法删除
        return false;
    }

    @Override
    public List<User> queryUsersByRoleCode(String roleCode) {
        Specification<User> spec = (root, criteriaQuery, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("isDelete").as(Boolean.class), false));

            // 过滤角色
            Path<UserRoleRef> userRolePath = root.join("userRoleRef", JoinType.LEFT);
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(userRolePath.get("role").get("roleCode").as(String.class), roleCode));

            criteriaQuery.distinct(true).where(predicate);
            return criteriaQuery.getRestriction();
        };
        return userRepository.findAll(spec);
    }

    @Override
    public void delete(User dbUser) {
        //软删除
//        dbUser.setIsDelete(true);
//        userRepository.save(dbUser);
        //硬删除
        userRepository.delete(dbUser);
    }

    @Override
    public List<String> queryAccount() {
        List<User> users = userRepository.findByIsDeleteFalse();
        ArrayList<String> accounts = new ArrayList<>();
        for (User user : users){
            accounts.add(user.getAccount());
        }
        return accounts;
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

}
