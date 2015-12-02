package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.GameBadge;
import models.GameBadgeAwarded;

public class GameBadgeVM {
    @JsonProperty("id") public Long id;
	@JsonProperty("badgeType") public String badgeType;
	@JsonProperty("name") public String name;
	@JsonProperty("description") public String description;
	@JsonProperty("icon") public String icon;
	@JsonProperty("seq") public int seq;
	
	@JsonProperty("awarded") public Boolean awarded;
	@JsonProperty("awardedDate") public Long awardedDate;
	
	public GameBadgeVM(GameBadge badge) {
	    this(badge, null);
	}
	
    public GameBadgeVM(GameBadge badge, GameBadgeAwarded badgeAwarded) {
        this.id = badge.id;
        this.badgeType = badge.badgeType.name();
        this.name = badge.name;
        this.description = badge.description;
        this.seq = badge.seq;
        
        if (badgeAwarded != null) {
            this.awarded = true;
            this.icon = badge.icon;
            this.awardedDate = badgeAwarded.getCreatedDate().getTime();
        } else {
            this.awarded = false;
            this.icon = badge.icon2;
            this.awardedDate = -1L;
        }
    }
}