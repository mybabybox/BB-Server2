package viewmodel;

import models.Country;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CountryVM {
	@JsonProperty("name") public String name;
	@JsonProperty("code") public String code;
	@JsonProperty("icon") public String icon;
	
	public CountryVM(Country country) {
		this.name = country.name;
		this.code = country.code.name();
		this.icon = country.icon;
	}
}
