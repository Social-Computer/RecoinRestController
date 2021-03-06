package main.java.sociam.pybossa.rest;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import sociam.pybossa.config.Config;
import sociam.pybossa.methods.MongodbMethods;

/**
 * 
 * @author user Saud Aljaloud
 * @author email sza1g10@ecs.soton.ac.uk
 *
 */
@Path("/sendTaskRun")
public class sendTaskRun {
	private static final Logger logger = Logger.getLogger(sendTaskRun.class);

	@GET
	@Produces("application/json")
	public Response insertTaskRun(@QueryParam("text") String text, @QueryParam("task_id") Integer task_id,
			@QueryParam("project_id") Integer project_id, @QueryParam("contributor_name") String contributor_name,
			@QueryParam("source") String source) throws JSONException {

		JSONObject data = new JSONObject();
		JSONObject status = new JSONObject();
		try {
			InputStream stream = sendTaskRun.class.getResourceAsStream("/log4j.properties");
			PropertyConfigurator.configure(stream);
			Config.reload();

			if (text != null && task_id != null && project_id != null && contributor_name != null && source != null) {
				logger.debug("receiving a GET request with the following data + text=" + text + " task_id=" + task_id
						+ " project_id=" + project_id + " contributor_name=" + contributor_name + " source=" + source);
				if (!(source.equals("TaskView") && text.contains("PRIO"))) {
					Document taskRun = MongodbMethods.getTaskRunsFromMongoDB(task_id, contributor_name, text);
					if (taskRun != null) {
						logger.error("You are only allowed one contribution for each task.");
						logger.error("task_id= " + task_id + " screen_name: " + contributor_name);
						status.put("message", "duplicate - no instruction added");
						status.put("status", "error");
						return Response.status(400).entity(status.toString()).build();
					}
				}
				Boolean isInserted = MongodbMethods.insertTaskRun(text, task_id, project_id, contributor_name, source);
				if (isInserted) {
					logger.info("TaskRun was inserted");

					data.put("text", text);
					data.put("task_id", task_id);
					data.put("project_id", project_id);
					data.put("contributor_name", contributor_name);
					data.put("source", source);
					status.put("data", data);
					status.put("status", "success");
					String result = status.toString();
					return Response.status(200).entity(result).build();
				} else {
					logger.error("Task run could not be inserted");
					status.put("message", "Did you already inserted the task run?");
					status.put("status", "error");
					return Response.status(500).entity(status.toString()).build();
				}

			} else {
				logger.error("All parameters should be provided");
				status.put("status", "Error: All parameters should be provided. text=" + text + "task_id=" + task_id
						+ " project_id=" + project_id + " contributor_name=" + contributor_name + " source=" + source);
				return Response.status(500).entity(status.toString()).build();
			}

		} catch (Exception e) {
			logger.error("Error", e);
			status = new JSONObject();
			status.put("status", "internal error: " + e);
			return Response.status(500).entity(status.toString()).build();
		}
	}

}
