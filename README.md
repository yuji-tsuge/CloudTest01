# HelloWorkloadApp

Welcome to the Hello Workload application!

This sample application demonstrates how to write a Hello World application leveraging the Workload Scheduler service and deploy it to Bluemix. This sample uses our new REST APIs and it is ready for deploying to the Bluemix cloud platform.

# Deploy to Bluemix

To begin:
- Click Deploy to Bluemix and log in with your Bluemix credentials

[![Deploy to Bluemix](https://bluemix.net/deploy/button.png)](https://bluemix.net/deploy?repository=https://github.com/WAdev0/HelloWorkloadSampleApp) 

1. Select your region.
2. Click Deploy.
3. After having been deployed, your app is NOT bound to the Workload Scheduler service.
- From Dashboard, select your app then:
1. from Connections box, click on Connect New
2. select Workload Scheduler service then Create
3. click on Restage
- Now your Hello Workload application is ready to run.

Otherwise, if you prefer use the CF cli, you could follow these steps: 
	- cf login -a https://api.ng.bluemix.net -u <your_username>
	- cf target -o <your_username> -s dev
	- cf push <your_app_name> -p helloWorkloadApp.war --no-start
	- cf create-service WorkloadScheduler Standard <your_service_name>
	- cf bind-service <your_app_name> <your_service_name>
	- cf restage <your_app_name>
	- cf start <your_app_name>
	
# Application overview:
In this application we use two folders:
  - src, which contains the source code of the app and the web content of the app
  - target, which contains the built war file

Src:

In src two folder are available: 
  - com 
  - webapp  
  
The first folder contains the java source code and the second contains the web resource of the app. The lower-level webapp folder contains
the page (index.jsp) that will be loaded when the app is launched. In the same folder is located the WEB-INF folder, containing  
the external library required for the project and the web.xml descriptor. 







