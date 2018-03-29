Get started with Hello Workload
-----------------------------------

Here the instructions if you use the command line cf instead of ACE
1. Install the cf command-line tool(https://www.ng.bluemix.net/docs/#cli/index.html#cli).
2. Extract the package and `cd` to it.
3. Connect to Bluemix:

		cf api https://api.ng.bluemix.net

4. Log into Bluemix:

		cf login -u <your_username>
		cf target -o <your_username> -s dev
				
5. Deploy your app:

		cf push <your_app_name> -p helloWorkloadApp.war --no-start
		cf create-service WorkloadScheduler Standard <your_service_name>		
		cf bind-service <your_app_name> <your_service_name>
		cf restage <your_app_name>
		cf start <your_app_name>

6. Access your app <your_app_name>
