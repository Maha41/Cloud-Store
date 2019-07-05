#!/bin/bash
#====================================================================================================
# Synopsis: This script is used to create CentOS ec2 instances with security group and Route 53
# in CloudFormation Stack
#====================================================================================================
# Creation of Elastic Compute Cloud stacks
#====================================================================================================
echo "Enter Stack Name for the creation of Ec2 Stack"
read sn
echo ""

ELB=$(aws elbv2 describe-load-balancers --query LoadBalancers[].{LoadBalancerArn:LoadBalancerArn} --output text)
echo $ELB

#====================================================================================================
# Validating the CloudFormation Template
#====================================================================================================

Valid=$(aws cloudformation  validate-template --template-body file://csye6225-cf-waf.json)
if [ $? -ne "0" ]
then
  echo "$Valid"
  echo "$StackName Template file to build infrastructure is NOT VALID....."
  exit 1
else
  echo " Proceed ahead. CloudFormation Template is VALID......"
  echo ""
fi

create=$(aws cloudformation create-stack --stack-name $sn --template-body file://csye6225-cf-waf.json --capabilities CAPABILITY_NAMED_IAM --parameters ParameterKey=MyLoadBalancer,ParameterValue=$ELB)

if [ $? -ne "0" ]
then
  echo "Creation of $sn stack failed...."
  exit 1
else
  echo "Creation of $sn is in progress......"
fi


#====================================================================================================
# Waiting for the stack to get created completely
#====================================================================================================

echo Stack in progress.....
Success=$(aws cloudformation wait stack-create-complete --stack-name $sn)

if [[ -z "$Success" ]]
then
  echo "$sn stack is created successfully. Printing output..."
else
  echo "Creation of $sn stack failed. Printing error and exiting....."
  echo "$Success"
  exit 1
fi