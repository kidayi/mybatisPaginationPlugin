package org.kidayi.mybatisPlugins.Pagination;

public interface IAppendLimit {
	public LimitSize getLimitSize();

	public void setLimitSize(LimitSize limitSize);
	
	public Object getExample();
}
