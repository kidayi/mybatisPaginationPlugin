package org.kidayi.mybatisPlugins.Pagination;

public interface IPage {
	public PageSize getPageSize();

	public void setPageSize(PageSize pageSize);
	
	public Object getExample();
}
