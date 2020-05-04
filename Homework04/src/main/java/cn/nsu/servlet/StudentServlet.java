package cn.nsu.servlet;

import cn.nsu.domain.PageBean;
import cn.nsu.domain.ResultInfo;
import cn.nsu.utils.JDBCUtils;
import cn.nsu.utils.JedisPoolUtils;
import com.alibaba.druid.support.json.JSONUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import redis.clients.jedis.Jedis;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@WebServlet("/student/*")
public class StudentServlet extends BaseServlet {

    //声明jedis
    private Jedis jedis = JedisPoolUtils.getJedis();
    //声明前端返回对象
    private ResultInfo info = new ResultInfo();

    /**
     * 分页处理
     * @param rows
     * @param currentPage
     * @return
     * @throws ServletException
     * @throws IOException
     */
    public PageBean page(Integer rows, Integer currentPage)
            throws ServletException, IOException {

        //定义一个list存放学生信息
        List list = new ArrayList<>();

        //计算起止索引
        int startIndex = (currentPage-1)*rows;
        int endIndex = startIndex+10;
        //计算总记录数
        Integer totalCount = jedis.zcard("student").intValue();
        //计算总页数
        int totalPage = (totalCount%rows)==0 ? (totalCount/rows) : (totalCount/rows)+1;

        //从redis中获取学生数据
        Set<String> students = jedis.zrange("student", startIndex, endIndex);
        for (String student : students) {
            Map<String,Object> map = (Map<String, Object>) JSONUtils.parse(student);
            list.add(map);
        }

        //封装PageBean
        PageBean<Object> pageBean = new PageBean<>();
        pageBean.setCurrentPage(currentPage);
        pageBean.setList(list);
        pageBean.setRows(rows);
        pageBean.setTotalCount(totalCount);
        pageBean.setTotalPage(totalPage);

        return pageBean;
    }

    /**
     * 查询所有
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void query(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //获取参数rows、currentPage
        Integer rows = Integer.parseInt(request.getParameter("rows"));
        Integer currentPage = Integer.parseInt(request.getParameter("currentPage"));

        //调用分页方法处理
        PageBean pageBean = page(rows, currentPage);

        //封装信息,并转化为json格式返回
        ObjectMapper mapper = new ObjectMapper();
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(mapper.writeValueAsString(pageBean));

    }

    /**
     * 新增
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void add(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //获取参数
        HashMap<String, Object> map = new HashMap<>();
        Double avgscore = Double.valueOf(Integer.parseInt(request.getParameter("avgscore")));
        map.put("id",request.getParameter("id"));
        map.put("name",request.getParameter("name"));
        map.put("birthday",request.getParameter("birthday"));
        map.put("description",request.getParameter("description"));
        map.put("avgscore",request.getParameter("avgscore"));
        //将学生信息转化为json存入redis
        String s = JSONUtils.toJSONString(map);
        jedis.zadd("student",avgscore,s);
        jedis.set(String.valueOf(new Double(avgscore).intValue())+map.get("name"),s);
        //重定向到数据展示页面
        response.sendRedirect("/Homework04/list.html");

    }

    /**
     * 删除
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void delete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //获取参数
        String score = request.getParameter("score");
        String name = request.getParameter("name");
        String key = score+name;
        //根据参数获取学生信息
        String s = jedis.get(key);
        try {
            //执行删除操作
            jedis.del(key);
            jedis.zrem("student",s);
            //封装前端返回消息
            info.setFlag(true);
        }catch (Exception e){
            info.setFlag(false);
        }finally {
            //将信息封装为json格式,并返回
            ObjectMapper mapper = new ObjectMapper();
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(mapper.writeValueAsString(info));
        }

    }

    /**
     * 修改
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void modify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //获取参数
        String key = request.getParameter("key");
        //根据key查询信息，并删除相关信息
        String s = jedis.get(key);
        jedis.del(key);
        jedis.zrem("student",s);
        //将回显数据返回
        response.getWriter().write(s);

    }

    /**
     * 初始化redis数据
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     */
    public void init(HttpServletRequest request, HttpServletResponse response)
            throws ParseException {

        //初始化一个jdbcTemplate
        JdbcTemplate jdbcTemplate = new JdbcTemplate(JDBCUtils.getDataSource());
        //编写查询语句
        String sql = "select * from student";
        //获取学生信息
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        //将数据存入redis
        for (Map<String, Object> map : list) {
            //得到平均分数
            Double avgscore = Double.valueOf((int) map.get("avgscore"));
            //转换日期格式
            map.put("birthday",new SimpleDateFormat("yyyy-MM-dd").format(map.get("birthday")));
            //将map转化为json格式
            String s = JSONUtils.toJSONString(map);
            //将数据存入sortedset中
            jedis.zadd("student",avgscore,s);
            //对应的，以平均分加学生姓名作为key，json数据作为value，创建string结构数据存入redis
            jedis.set(String.valueOf(new Double(avgscore).intValue())+map.get("name"),s);
        }
    }
}
