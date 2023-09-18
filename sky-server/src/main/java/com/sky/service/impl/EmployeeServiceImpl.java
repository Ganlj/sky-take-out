package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 用springBoot提供的工具类来进行MD5加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    @Override
    public void save(EmployeeDTO employeeDTO) {
        //在持久层做数据保存的时候还是将数据以Employee为载体传递给持久层
        //用BeanUtils里的方法来拷贝属性,前提是两个实体里的属性名和类型一样
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);
        //在这里employee还有更多的属性值，所以需要手动设置
        employee.setStatus(StatusConstant.ENABLE);//状态默认设置为1，表示启用账号
        //设置默认密码为123456的MD5加密，同样用到了DigestUtils的Md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        //设置创建时间
        //employee.setCreateTime(LocalDateTime.now());
        //设置更新时间
        //employee.setUpdateTime(LocalDateTime.now());
        //设置创建人id,即为当前登录用户的id
        //employee.setCreateUser(BaseContext.getCurrentId());
        //设置更新人id，即为当前登录用户的id
        //employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.insert(employee);
    }

    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //对表的分页查询在service进行，相当于在service里定义查询的页数和页码，然后再调用mapper接口单表查询
        //传入查询的页数和一页多少条
        //PageHelper相当于一个mybatis底层的拦截器
        PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize());
        //遵守规范，返回Page对象
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        long total = page.getTotal();
        List<Employee> records = page.getResult();
        return new PageResult(total,records);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        //将mapper层的update方法更加灵活的使用，不单单局限于更新状态
        //所以将参数封装成Employee对象
        Employee employee = Employee.builder().status(status).id(id).build();
        employeeMapper.update(employee);
        
    }

    @Override
    public Employee getById(Long id) {
        //查询出来的密码安全起见，将其设置为******
        Employee employee = employeeMapper.getById(id);
        employee.setPassword("*********");
        return employee;
    }

    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
                                //源对象      目标对象
        BeanUtils.copyProperties(employeeDTO,employee);
        //设置更新时间和更新人
        //employee.setUpdateTime(LocalDateTime.now());
        //employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.update(employee);
    }

}
