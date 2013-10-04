//copying the conf files of master to all the nodes

//now your cluster is setup!

To run the program:

Step1: Copy ish-history.csv file into dfs
//copy ish-history.csv into dfs from hadoop folder
bin/hadoop dfs -copyFromLocal /mnt/ish-history.csv /ish-history.csv

Step2: Copy the dataset into dfs
//copy files from mounted dataset into dfs
bin/hadoop dfs -copyFromLocal /mnt/gsod/* /weatherdata

Step3: Run the SolarEnergy program
//Command to run the SolarEnergy.jar
bin/hadoop jar SolarEnergy.jar cloud.SolarEnergy /weatherdata/2008/ /Solaroutput1 /Solaroutput2

Step3: Run the SolarEnergy program
//Command to run the WindEnergy.jar
bin/hadoop jar WindEnergy.jar cloud.WindEnergy /weatherdata/2008/ /Windoutput1 /Windoutput2


PLEASE NOTE: Ww have two different programs for SolarEnergy and WindEnergy since we had divided the work within us.