#!/bin/bash

echo "Starting with Stack creation process......"
echo "Validating the Template......"

aws cloudformation validate-template --template-body file://csye6225-cf-application.json #>/dev/null 2>&1

if [ $? -eq 0 ]
then
    echo "Template validation successful"
    echo "Fetching Ami Id now ......"
    amiId=$(aws ec2 describe-images --filters "Name=tag:Name,Values=csye6225-CentOs" --query 'Images[0].{ID:ImageId}' --output text)
    if [ $? -eq 0 ]
    then
       echo "Successfully fetched the Ami ID"
       echo "amiId= $amiId"
    else
       echo "not able to fetch Ami Id"
       exit 1
    fi
    read -p "please enter the stackname used previously for vpc and its resources: " vpcStack
    vpcId=$(aws ec2 describe-vpcs   --query 'Vpcs[*].{VpcId:VpcId}' --filters Name=tag:Name,Values=$vpcStack-csye6225-vpc Name=is-default,Values=false --output text  2>&1)
    echo "VpcId : $vpcId"
    #subnet1
    nameExtension1="-csye6225-subnet1"
    subnetName1=$vpcStack$nameExtension1
    echo "Public Subnet name : $subnetName1"
    Subnet1=$(aws ec2 describe-subnets --filters "Name=tag:Name,Values=$subnetName1"  --query 'Subnets[*].SubnetId' --output text 2>&1)
    echo "SubnetId1: $Subnet1"
    #subnet2
    nameExtension2="-csye6225-subnet2"
    subnetName2=$vpcStack$nameExtension2
    echo "Public Subnet name : $subnetName2"
    Subnet2=$(aws ec2 describe-subnets --filters "Name=tag:Name,Values=$subnetName2"  --query 'Subnets[*].SubnetId' --output text 2>&1)
    echo "SubnetId2: $Subnet2"
    #subnet3
    nameExtension3="-csye6225-subnet3"
    subnetName3=$vpcStack$nameExtension3
    echo "Public Subnet name : $subnetName3"
    Subnet3=$(aws ec2 describe-subnets --filters "Name=tag:Name,Values=$subnetName3"  --query 'Subnets[*].SubnetId' --output text 2>&1)
    echo "SubnetId3: $Subnet3"

    zone=$(aws route53 list-hosted-zones --query HostedZones[].{Name:Name} --output text | sed 's/.$//')
    bucket="code-deploy.$zone"
    echo "bucketName = $bucket"
   
    bucketKey="lamda-1.0-SNAPSHOT.zip"
    echo "bucketKey = $bucketKey"

    domainName=$zone
    echo "Domain = $domainName"   

    read -p "please enter the Stackname: " stackname
    echo "creating stack $stackname now ......"
    #aws cloudformation create-stack --stack-name $stackname --template-body file://csye6225-cf-application.json --parameters ParameterKey=amiId,ParameterValue=$amiId ParameterKey=myVpc,ParameterValue=vpc-ae398bd4 ParameterKey=subnetId1,ParameterValue=subnet-4f107213 ParameterKey=subnetId2,ParameterValue=subnet-56187478 ParameterKey=subnetId3,ParameterValue=subnet-6c6e0f0b
    aws cloudformation create-stack --stack-name $stackname --template-body file://csye6225-cf-application.json --parameters ParameterKey=amiId,ParameterValue=$amiId ParameterKey=myVpc,ParameterValue=$vpcId ParameterKey=subnetId1,ParameterValue=$Subnet1 ParameterKey=subnetId2,ParameterValue=$Subnet2 ParameterKey=subnetId3,ParameterValue=$Subnet3 ParameterKey=bucketName,ParameterValue=$bucket ParameterKey=attachmentBucket,ParameterValue=$zone ParameterKey=bucketKey,ParameterValue=$bucketKey ParameterKey=domainName,ParameterValue=$domainName --capabilities CAPABILITY_NAMED_IAM
    if [ $? -eq 0 ]
    then
        while true;
        do
            completionCheck=$(aws cloudformation describe-stacks --stack-name $stackname --query "Stacks[0].StackStatus")
            if [ $completionCheck = "\"CREATE_COMPLETE\"" ]
            then
                break
            else
                echo -n "."
            fi
        done
        echo "."
        echo "stack $stackname created Successfully"
    else
        errorCheck=$(aws cloudformation describe-stacks --stack-name $stackname --query "Stacks[0].StackStatus")
        if [ $errorCheck = "\"CREATE_COMPLETE\"" ]
        then
            echo "Stack: $stackname already exists"
        else
            echo "unable to create stack $stackname"
        fi
    fi
else
    echo "Unable to validate template"
fi

