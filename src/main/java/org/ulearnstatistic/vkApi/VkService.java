package org.ulearnstatistic.vkApi;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.users.Fields;
import com.vk.api.sdk.objects.users.responses.SearchResponse;

public class VkService {
    private final long APP_ID;
    private final String CODE;
    private final VkApiClient vk;
    private final UserActor actor;

    public VkService() {
        APP_ID = VkConfig.APP_ID;
        CODE = VkConfig.CODE;

        TransportClient transportClient = new HttpTransportClient();
        vk = new VkApiClient(transportClient);
        actor = new UserActor(APP_ID,CODE);
    }

    /**
     * Поиск всех пользователей по имени среди группы.
     * @param name Имя пользователя
     * @param subId Группа, в которой будет осуществляться поиск
     * @return SearchResponse, содержащий необходимые сведения для найденных пользователей
     * @throws ClientException
     * @throws ApiException
     */
    public SearchResponse getUserByNameAndSubs(String name, long subId) throws ClientException, ApiException {
        return vk.users().search(actor)
                .q(name)
                //.count(10)
                .fields(Fields.BDATE,Fields.SEX,Fields.CITY,Fields.UNIVERSITIES,Fields.OCCUPATION,Fields.HOME_TOWN,Fields.EDUCATION)
                .groupId(subId)
                .execute();
    }
}
