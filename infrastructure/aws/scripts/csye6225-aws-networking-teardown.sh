#!/bin/bash

read -p 'Stack name: ' STACK_NAME


vpc_id=$(aws ec2 describe-vpcs --filters "Name=tag-value,Values="$STACK_NAME-csye6225-vpc"" --query "Vpcs[*].VpcId" --output text)
if [ "$vpc_id" == "" ]; then
	echo "[error] VPC not found" 1>&2
	exit 1
fi
echo ${vpc_id}

subnet=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=${vpc_id})
subnetid1=$(echo -e "$subnet" | jq '.Subnets[0].SubnetId' | tr -d '"')
if [ "$subnetid1" == "" ]; then
	echo "[error] subnet 1 not found" 1>&2
	exit 1
fi
subnetid2=$(echo -e "$subnet" | jq '.Subnets[1].SubnetId' | tr -d '"')
if [ "$subnetid2" == "" ]; then
	echo "[error] subnet 2 not found" 1>&2
	exit 1
fi
subnetid3=$(echo -e "$subnet" | jq '.Subnets[2].SubnetId' | tr -d '"')
if [ "$subnetid3" == "" ]; then
	echo "[error] subnet 3 not found" 1>&2
	exit 1
fi

aws ec2 delete-subnet --subnet-id $subnetid1
if [ "$?" != "0" ]; then
    echo "Couldn't delete subnet 1"
    exit 77
fi
aws ec2 delete-subnet --subnet-id $subnetid2
if [ "$?" != "0" ]; then
    alias
    exit 77
fi

aws ec2 delete-subnet --subnet-id $subnetid3
if [ "$?" != "0" ]; then
    echo "Couldn't delete subnet 3"
    exit 77
fi
routeid=$(aws ec2 describe-route-tables --filters "Name=tag-value,Values="$STACK_NAME-csye6225-rt"" --query "RouteTables[*].RouteTableId" --output text)

#route=$(aws ec2 describe-route-tables --filters Name=vpc-id,Values=${vpc_id})
#routeid=$(echo -e "$route" | jq '.RouteTables[1].RouteTableId' | tr -d '"')

echo "delete route"
aws ec2 delete-route --route-table-id $routeid --destination-cidr-block 0.0.0.0/0
if [ "$?" != "0" ]; then
    echo "Couldn't delete route"
    exit 77
else
	echo "delete route table"
    aws ec2 delete-route-table --route-table-id $routeid
    if [ "$?" != "0" ]; then
        echo "Couldn't delete route table"
        exit 77
    else
        gateway_id=$(aws ec2 describe-internet-gateways --filters "Name=tag-value,Values="$STACK_NAME"" --query "InternetGateways[*].InternetGatewayId" --output text)
        echo "detach"
        aws ec2 detach-internet-gateway --internet-gateway-id $gateway_id --vpc-id $vpc_id
          if [ "$?" != "0" ]; then
            echo "Couldn't disassociate internet gateway"
            exit 77
           else
            echo "delete internet gateway"
            aws ec2 delete-internet-gateway --internet-gateway-id $gateway_id
             if [ "$?" != "0" ]; then
                echo "Couldn't delete internet gateway"
                exit 77
             else
                echo "delete vpc"
                aws ec2 delete-vpc --vpc-id $vpc_id
                if [ "$?" != "0" ]; then
                    echo "Couldn't delete vpc"
                    exit 77
                 else
                    echo "VPC and all it's dependencies are deleted successfully"
                fi
             fi
          fi
     fi

fi

exit 0