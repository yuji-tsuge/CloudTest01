/*********************************************************************
 *
 * Licensed Materials - Property of IBM
 * Product ID = 5698-WSH
 *
 * Copyright IBM Corp. 2015. All Rights Reserved.
 *
 ********************************************************************/
package com.ibm.twa.bluemix.samples;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.tws.api.ApiClient;
import com.ibm.tws.api.ApiException;
import com.ibm.tws.api.ProcessApi;
import com.ibm.tws.api.ProcessHistoryApi;
import com.ibm.tws.api.ProcessLibraryApi;
import com.ibm.tws.model.Process;
import com.ibm.tws.model.ProcessHistoryInstance;
import com.ibm.tws.model.ProcessLibrary;
import com.ibm.tws.model.RestAction;
import com.ibm.tws.model.RestAuthenticationData;
import com.ibm.tws.model.RestInput;
import com.ibm.tws.model.RestfulStep;
import com.ibm.tws.model.Step;
import com.ibm.tws.model.StepHistoryInstance;

public class HelloWorkload {
	// Once connected and authenticated correctly, this boolean is true
	boolean connected = false;
	// Monitor navigation history
	boolean tracked = false;
	// Holds the last created process id.
	Integer myProcessId = -1;	
	// If true adds <br> instead of \n for each output row
	boolean htmlOut = true;

	String workloadServiceName = "WorkloadScheduler";
	String agentName = "";
	String agentName_suffix = "_CLOUD";

	final String engineName = "engine";
	final String engineOwner = "engine";

	String restURL = "";
	String user = "";
	String password = "";
	String tenantId = ""; 


	ApiClient apiClient;

	final String LIBRARY_NAME = "HelloWorkload";
	final String PROCESS_NAME = "HelloWorld";

	// Default empty constructor.
	public HelloWorkload() {
	};

	/**
	 * Connects and authenticates to the server, exploring the content of
	 * VCAP_SERVICES content.
	 * 
	 * @param o : Output Stream to write useful info
	 */
	public void helloWorkloadConnect(Writer o) throws JSONException {
		PrintWriter out = new PrintWriter(o);
		String vcapJSONString = System.getenv("VCAP_SERVICES");

		Object jsonObject = JSON.parse(vcapJSONString);
		JSONObject json = (JSONObject) jsonObject;
		String key;
		JSONArray twaServiceArray = null;
		println(out, "Looking for Workload Automation Service...");
		out.flush();

		for (Object k : json.keySet()) {
			key = (String) k;
			if (key.startsWith(workloadServiceName)) {
				twaServiceArray = (JSONArray) json.get(key);
				println(out, "Workload Automation service found!");
				out.flush();
				break;
			}
		}
		if (twaServiceArray == null) {
			println(out,
					"Could not connect: I was not able to find the Workload Automation service!");
			println(out, "This is your VCAP services content");
			println(out, vcapJSONString);
			out.flush();
			return;
		}

		JSONObject twaService = (JSONObject) twaServiceArray.get(0);
		JSONObject credentials = (JSONObject) twaService.get("credentials");

		println(out, "Starting Workload Automation connection..");
		out.flush();

		String url = (String) credentials.get("url");
		user = (String) credentials.get("userId");
		password = (String) credentials.get("password");
		String basePath = "";
		try {
			user = URLDecoder.decode(user, "UTF-8");
			password = URLDecoder.decode(password, "UTF-8");
			basePath = "https://" + url.substring(url.indexOf("@") + 1, url.indexOf("?"));			
			restURL = basePath.substring(0, basePath.indexOf("ibm/")) + "twsd/plan";
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		int index = url.indexOf("tenantId=") + 9;
		String prefix = url.substring(index, index + 2);
		println(out, "prefix=" + prefix);
		tenantId = prefix;
		agentName = prefix + agentName_suffix;

		try  {
			HelloWorkload.disableCertificateValidation();
			apiClient = new ApiClient();		
			apiClient.setBasePath(basePath);
			apiClient.setUsername(user);
			apiClient.setPassword(password);

		} catch (Exception e) {
			println(out, "Could not connect to the service: "
					+ e.getClass().getName() + " " + e.getMessage());
			out.flush();
			return;
		}
		connected = true;
		println(out, "Connection obtained.");
		out.flush();
	}

	/**
	 * Creates a very simple hello world process
	 * 
	 * @param o
	 * @throws ApiException
	 */
	public void helloWorkloadCreate(Writer o) throws ApiException {
		PrintWriter out = new PrintWriter(o);		

		ProcessApi processApi = new ProcessApi(apiClient);
		ProcessLibraryApi libApi = new ProcessLibraryApi(apiClient);

		// Determine Library
		// Libary name must be unique in your tenantID
		List<ProcessLibrary> libraries = libApi.listProcessLibrary(tenantId, engineName, engineOwner);
		boolean present = false;
		Integer libID = null;
		for (ProcessLibrary l: libraries) {
			if (l.getName().equals(LIBRARY_NAME)) {
				present = true;
				libID = l.getId();
			}
		}
		if (!present) {
			ProcessLibrary processLib = new ProcessLibrary();
			processLib.setName(LIBRARY_NAME);
			List<ProcessLibrary> result = libApi.createProcessLibrary(processLib, tenantId, engineName, engineOwner);
			if (result.isEmpty())
				throw new RuntimeException("Could not create library");
			libID = result.get(0).getId();
		}

		Process process = new Process();
		process.setName(PROCESS_NAME);
		process.setProcesslibraryid(libID);
		process.setProcessstatus(false);

		List<Step> steps = new ArrayList<Step>();
		Step step = new Step();

		RestfulStep restfulStep = new RestfulStep();
		restfulStep.agent(agentName);

		RestAction ra = new RestAction();
		ra.setUri(restURL);
		ra.setMethod("GET");

		restfulStep.setAction(ra);		

		RestAuthenticationData rad = new RestAuthenticationData();
		rad.setUsername(user);
		rad.setPassword(password);
		restfulStep.setAuthdata(rad);

		RestInput ri = new RestInput();
		ri.setIsFile(false);
		ri.setInput("");
		restfulStep.setInput(ri);		

		step.setRestfulStep(restfulStep);

		steps.add(step);
		process.setSteps(steps);

		try {
			println(out, "Creating and enabling the process. This process may take a while");
			out.flush();
			Process procResult = processApi.createProcess(process, tenantId,
					engineName, engineOwner);
			if (procResult != null) {				
				myProcessId = procResult.getId();
				String id = Integer.toString(procResult.getId());
				processApi.toggleProcessStatus(id, tenantId, engineName, engineOwner);				
				println(out, "Running the process....");
				out.flush();
				processApi.runNowProcess(id, tenantId, "", engineName,	engineOwner);
			}
			else {
				throw new RuntimeException("Could not run null process");
			}

		} catch (Exception e) {
			println(out, "Could not connect complete the operation: "
					+ e.getClass().getName() + " " + e.getMessage());
			out.flush();

		}

	}

	/**
	 * Tracks a process created with a previous call to helloWorkloadCreate
	 * 
	 * @param o
	 */
	public void helloWorkloadTrack(Writer o) {
		tracked = true;
		PrintWriter out = new PrintWriter(o);
		String myProcessId_str = Long.toString(this.myProcessId);
		try {
			ProcessHistoryApi ops = new ProcessHistoryApi(apiClient);
			List<ProcessHistoryInstance> list = ops.listProcessHistory(
					myProcessId_str, tenantId, engineName,
					engineOwner);
			for (ProcessHistoryInstance processHistoryInstance : list) {
				println(out, "Process details:\n");
				println(out,
						"Started: " + processHistoryInstance.getStartdate()
						+ "\n Completed steps: "
						+ processHistoryInstance.getCompletedsteps()
						+ "\n Is completed: "
						+ (processHistoryInstance.getStatus() == 0)
						+ "\n");

				if ((processHistoryInstance.getStatus() == 0)) {
					println(out, "The process has completed all the steps in: "
							+ processHistoryInstance.getElapsedtime() / 1000
							+ " seconds\n");
					List<StepHistoryInstance> steps = ops.listSteps(myProcessId_str, processHistoryInstance.getId(), tenantId, false, engineName, engineOwner);
					for (StepHistoryInstance stepsInstance : steps) {
						String stepLog = ops.getStepLog(myProcessId_str, processHistoryInstance.getId(), stepsInstance.getId(), tenantId, engineName, engineOwner);
						println(out, "Step Log:");
						println(out, stepLog);
						out.flush();
					}
				}
				out.flush();
			}

		} catch (Exception e) {
			out.println("Could not connect complete the operation: "
					+ e.getClass().getName() + " " + e.getMessage());
			out.flush();
		}
	}

	public void println(PrintWriter out, String msg) {
		if (this.htmlOut) {
			out.print(msg.replaceAll("\\n", "<br/>") + "<br/>");
		} else {
			out.println(msg);
		}
	}

	public Integer getMyProcessId() {
		return myProcessId;
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean tracked() {		
		return tracked;
	}

	public void resetAll() {
		connected = false;
		myProcessId = -1;
		tracked = false;		
	}

	/**
	 * Disables https certificate validation
	 */
	private static void disableCertificateValidation() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

			@Override
			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
				// TODO Auto-generated method stub

			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				// TODO Auto-generated method stub
				return new X509Certificate[0];
			}
		} };

		// Ignore differences between given hostname and certificate hostname
		HostnameVerifier hv = new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				// TODO Auto-generated method stub
				return true;
			}
		};

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection
			.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (Exception e) {
		}
	}
}
