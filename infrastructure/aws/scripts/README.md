# CSYE 6225 - Spring 2019

## Cloud Formation

## Description
This is the template to deploy amazon cloudformation. 
This csye6225-cf-networking.json file is passed as a parameter to create stack.
The csye6225-cf-parameteres.json contains list of variable parameters that can be updated by the user


## Command to create stack 
`./csye6225-aws-networking-setup.json` 
 or
`bash csye6225-aws-networking-setup.sh`

Then follow the prompt, to enter stack name : 
`
Please Enter the Following
VPC_NAME="MyVPC"
SUBNET 1 NAME="Mysubnet1"
SUBNET 2 NAME="Mysubnet2
SUBNET 3 NAME="Mysubnet3`

## Command to terminate stack 
`./csye6225-aws-networking-stack.json` 
 or
`bash csye6225-aws-networking-teardown.json`


Then follow the prompt, to enter stack name : 

`Stack name: myVPC
`