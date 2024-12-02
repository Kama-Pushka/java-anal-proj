package org.ulearnstatistic.vkApi;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.users.responses.SearchResponse;
import org.ulearnstatistic.model.Student;

import java.util.HashMap;
import java.util.List;

public class VkRequest {
    public static HashMap<String, SearchResponse> searchStudentsByGroups(VkRepository vk, List<Student> students, long[] groupId) {
        var studentDct = new HashMap<String, SearchResponse>(); // TODO заменить на ID
        for (var student : students) {
            var name = student.getName();
            for (var i = 0; i < groupId.length && studentDct.get(name) == null; i++) {
                try {
                    studentDct.put(student.getName(), vk.getUserByNameAndSubs(student.getName(), groupId[i]));
                } catch (ApiException e) {
                    e.printStackTrace(); // TODO обработка капчи
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return studentDct;
    }
}
