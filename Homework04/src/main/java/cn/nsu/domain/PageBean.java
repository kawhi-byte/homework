package cn.nsu.domain;

import java.util.List;

public class PageBean<T> {

    private int totalCount; //总记录数
    private int totalPage;  //总页数
    private int currentPage;  //当前页数
    private int rows;        //每页显示的条数
    private List<T> list;    //每页显示的数据

    public PageBean() { }

    public PageBean(int totalCount, int totalPage, int currentPage, int rows, List<T> list) {
        this.totalCount = totalCount;
        this.totalPage = totalPage;
        this.currentPage = currentPage;
        this.rows = rows;
        this.list = list;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "PageBean{" +
                "totalCount=" + totalCount +
                ", totalPage=" + totalPage +
                ", currentPage=" + currentPage +
                ", rows=" + rows +
                ", list=" + list +
                '}';
    }
}
