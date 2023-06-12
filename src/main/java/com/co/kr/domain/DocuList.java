package com.co.kr.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderMethodName="builder")
public class DocuList {
	private String bdTitle;
	private String bdContent;
	private String bdUpdateAt;
	private String mbLevel;
	private int bdSeq;
}
