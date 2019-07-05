# CSYE 6225 - Spring 2019

## Cloud Formation

## Description
This is the template to deploy amazon cloudformation. 
This csye6225-cf-networking.json file is passed as a parameter to create stack.
The csye6225-cf-parameteres.json contains list of variable parameters that can be updated by the user


## Command to create stack 
`./csye6225-aws-cf-create-stack.json` 
 or
`bash csye6225-aws-cf-create-stack.json`

Then follow the prompt, to enter stack name : 

`Starting with Stack creation process......
 Validating the Template......
 Template validation successful
 please enter the Stackname: vpc
`

## Command to terminate stack 
`./csye6225-aws-cf-terminate-stack.json` 
 or
`bash csye6225-aws-cf-terminate-stack.json`


Then follow the prompt, to enter stack name : 

`Starting with Stack creation process......
 Validating the Template......
 Template validation successful
 please enter the Stackname: vpc
`


