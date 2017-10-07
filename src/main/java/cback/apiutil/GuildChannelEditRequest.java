package cback.apiutil;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class GuildChannelEditRequest {

    public static class Builder {

        private Integer position;
        private Long id;

        public Builder id(Long id){
            this.id = id;
            return this;
        }
        public Builder position(int position) {
            this.position = position;
            return this;
        }
        public GuildChannelEditRequest build() {
            return new GuildChannelEditRequest(id, position);
        }
    }


    private Long id;
    private Integer position;

    GuildChannelEditRequest(Long id, Integer position) {
        this.id = id;
        this.position = position;
    }

    GuildChannelEditRequest() {
    }

}