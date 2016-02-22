package org.kidayi.mybatisPlugins.Pagination;



public class PageSize {
	private long count=0;
	
	private long pageSize=10;
	
	private long pageCountMax;
	
	private long pageNumInput;
	
	private long pageNumBack;
	
	private long start;
	
	public void pageCalculate(){
		if(this.pageSize==0){
			this.pageSize=Constants.page_size;
		}
		
		this.pageCountMax=this.count/this.pageSize;
		if(this.count%this.pageSize>0){
			this.pageCountMax++;
		}
		
		if(this.pageCountMax<1){
			this.pageCountMax=1;
		}
		
		if(this.pageNumInput<1){
			this.pageNumBack=1;
		}else if(this.pageNumInput>this.pageCountMax){
			this.pageNumBack=this.pageCountMax;
		}else{
			this.pageNumBack=this.pageNumInput;
		}
		
		this.start=(this.getPageNumBack()-1)*this.pageSize;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public long getPageSize() {
		return pageSize;
	}

	public void setPageSize(long pageSize) {
		this.pageSize = pageSize;
	}

	public long getPageCountMax() {
		return pageCountMax;
	}

	public void setPageCountMax(long pageCountMax) {
		this.pageCountMax = pageCountMax;
	}

	public long getPageNumInput() {
		return pageNumInput;
	}

	public void setPageNumInput(long pageNumInput) {
		this.pageNumInput = pageNumInput;
	}

	public long getPageNumBack() {
		return pageNumBack;
	}

	public void setPageNumBack(long pageNumBack) {
		this.pageNumBack = pageNumBack;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}
	
	
}
