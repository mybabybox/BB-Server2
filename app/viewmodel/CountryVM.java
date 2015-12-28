package viewmodel;

import models.Country;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CountryVM {
    @JsonProperty("id") public Long id;
	@JsonProperty("name") public String name;
	@JsonProperty("code") public String code;
	@JsonProperty("icon") public String icon;
	@JsonProperty("seq") public int seq;
	
	public CountryVM(Country country) {
	    this.id = country.id;
		this.name = country.name;
		this.code = country.code.name();
		this.icon = country.icon;
		this.seq = country.seq;
	}
}
