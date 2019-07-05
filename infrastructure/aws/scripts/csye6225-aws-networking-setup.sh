#!/bin/bash
echo 'Please Enter the Following'
#

read -p 'Stack Name: ' VPC_NAME
read -p 'Subnet 1 NAME: ' SUBNET_PUBLIC_NAME1
read -p 'Subnet 2 NAME: ' SUBNET_PUBLIC_NAME2
read -p 'Subnet 3 NAME: ' SUBNET_PUBLIC_NAME3

#cidr block
VPC_CIDR="191.0.0.0/16"
SUBNET_PUBLIC_CIDR1="191.0.0.0/24"
SUBNET_PUBLIC_1="us-east-1a"
SUBNET_PUBLIC_CIDR2="191.0.1.0/24"
SUBNET_PUBLIC_2="us-east-1b"
SUBNET_PUBLIC_CIDR3="191.0.2.0/24"
SUBNET_PUBLIC_3="us-east-1c"

# Create VPC
echo "Creating VPC in preferred region..."
VPC_ID=$(aws ec2 create-vpc \
  --cidr-block $VPC_CIDR \
  --query 'Vpc.{VpcId:VpcId}' \
  --output text )
if [ "$VPC_ID" == "" ]; then
	echo "[error] creating vpc failed!" 1>&2
	exit 1
fi
echo "  VPC ID '$VPC_ID' "

# Add Name tag to VPC
aws ec2 create-tags \
  --resources $VPC_ID \
  --tags "Key=Name,Value=$VPC_NAME-csye6225-vpc"
echo "  VPC ID '$VPC_ID' NAMED as $VPC_NAME-csye6225-vpc."

# Create Public Subnet 1
echo "Creating Public Subnet 1..."
SUBNET_PUBLIC_ID1=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block $SUBNET_PUBLIC_CIDR1 \
  --availability-zone $SUBNET_PUBLIC_1 \
  --query 'Subnet.{SubnetId:SubnetId}' \
  --output text )
if [ "$SUBNET_PUBLIC_ID1" == "" ]; then
	echo "[error] creating subnet1 failed!" 1>&2
	exit 1
fi
echo "  Subnet ID '$SUBNET_PUBLIC_ID1' CREATED in '$SUBNET_PUBLIC_1'" \
  "Availability Zone."

# Add Name tag to Public Subnet 1
aws ec2 create-tags \
  --resources $SUBNET_PUBLIC_ID1 \
  --tags "Key=Name,Value=$SUBNET_PUBLIC_NAME1"
echo "  Subnet ID '$SUBNET_PUBLIC_ID1' NAMED as" \
  "'$SUBNET_PUBLIC_NAME1'."

# Create Public Subnet 2
echo "Creating Public Subnet 2..."
SUBNET_PUBLIC_ID2=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block $SUBNET_PUBLIC_CIDR2 \
  --availability-zone $SUBNET_PUBLIC_2 \
  --query 'Subnet.{SubnetId:SubnetId}' \
  --output text )
if [ "$SUBNET_PUBLIC_ID2" == "" ]; then
	echo "[error] creating subnet2 failed!" 1>&2
	exit 1
fi
echo "  Subnet ID '$SUBNET_PUBLIC_ID2' CREATED in '$SUBNET_PUBLIC_2'" \
  "Availability Zone."

# Add Name tag to Public Subnet 2
aws ec2 create-tags \
  --resources $SUBNET_PUBLIC_ID2 \
  --tags "Key=Name,Value=$SUBNET_PUBLIC_NAME2"
echo "  Subnet ID '$SUBNET_PUBLIC_ID2' NAMED as" \
  "'$SUBNET_PUBLIC_NAME2'."

# Create Public Subnet 3
echo "Creating Public Subnet 3..."
SUBNET_PUBLIC_ID3=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block $SUBNET_PUBLIC_CIDR3 \
  --availability-zone $SUBNET_PUBLIC_3 \
  --query 'Subnet.{SubnetId:SubnetId}' \
  --output text )
if [ "$SUBNET_PUBLIC_ID3" == "" ]; then
	echo "[error] creating subnet3 failed!" 1>&2
	exit 1
fi
echo "  Subnet ID '$SUBNET_PUBLIC_ID3' CREATED in '$SUBNET_PUBLIC_3'" \
  "Availability Zone."

# Add Name tag to Public Subnet 3
aws ec2 create-tags \
  --resources $SUBNET_PUBLIC_ID3 \
  --tags "Key=Name,Value=$SUBNET_PUBLIC_NAME3"
echo "  Subnet ID '$SUBNET_PUBLIC_ID3' NAMED as" \
  "'$SUBNET_PUBLIC_NAME3'."


# Create Internet gateway
echo "Creating Internet Gateway..."
IGW_ID=$(aws ec2 create-internet-gateway \
  --query 'InternetGateway.{InternetGatewayId:InternetGatewayId}' \
  --output text )
if [ "$IGW_ID" == "" ]; then
	echo "[error] creating Internet Gateway  failed!" 1>&2
	exit 1
fi
echo "  Internet Gateway ID '$IGW_ID' CREATED."

# Add Name tag to Internet Gateway
aws ec2 create-tags \
  --resources $IGW_ID \
  --tags "Key=Name,Value=$VPC_NAME"
echo "  Subnet ID '$IGW_ID' NAMED as" \
  "'$VPC_NAME'."

# Attach Internet gateway to your VPC
aws ec2 attach-internet-gateway \
  --vpc-id $VPC_ID \
  --internet-gateway-id $IGW_ID
echo "  Internet Gateway ID '$IGW_ID' ATTACHED to VPC ID '$VPC_ID'."

# Create Route Table
echo "Creating Route Table..."
ROUTE_TABLE_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --query 'RouteTable.{RouteTableId:RouteTableId}' \
  --output text )
if [ "$ROUTE_TABLE_ID" == "" ]; then
	echo "[error] creating Route Table  failed!" 1>&2
	exit 1
fi
echo "  Route Table ID '$ROUTE_TABLE_ID' CREATED."

# Add Name tag to Route table
aws ec2 create-tags \
  --resources $ROUTE_TABLE_ID \
  --tags "Key=Name,Value=$VPC_NAME-csye6225-rt"
echo "  Subnet ID '$ROUTE_TABLE_ID' NAMED as" \
  "$VPC_NAME-csye6225-rt."

# Create route to Internet Gateway
RESULT=$(aws ec2 create-route \
  --route-table-id $ROUTE_TABLE_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $IGW_ID )
if [ "$RESULT" == "" ]; then
	echo "[error] creating Route to Internet Gateway  failed!" 1>&2
	exit 1
fi
echo "  Route to '0.0.0.0/0' via Internet Gateway ID '$IGW_ID' ADDED to" \
  "Route Table ID '$ROUTE_TABLE_ID'."

# Associate Subnet 1 with Route Table
RESULT=$(aws ec2 associate-route-table  \
  --subnet-id $SUBNET_PUBLIC_ID1 \
  --route-table-id $ROUTE_TABLE_ID )
if [ "$RESULT" == "" ]; then
	echo "[error] associating Subnet1 with route Table  failed!" 1>&2
	exit 1
fi
echo "Subnet ID '$SUBNET_PUBLIC_ID1' ASSOCIATED with Route Table ID" \
  "'$ROUTE_TABLE_ID'."

# Associate Subnet 2 with Route Table
RESULT=$(aws ec2 associate-route-table  \
  --subnet-id $SUBNET_PUBLIC_ID2 \
  --route-table-id $ROUTE_TABLE_ID )
 if [ "$RESULT" == "" ]; then
	echo "[error] associating Subnet2 with route Table  failed!" 1>&2
	exit 1
fi
echo "Subnet ID '$SUBNET_PUBLIC_ID2' ASSOCIATED with Route Table ID" \
  "'$ROUTE_TABLE_ID'."

# Associate Subnet 3 with Route Table
RESULT=$(aws ec2 associate-route-table  \
  --subnet-id $SUBNET_PUBLIC_ID3 \
  --route-table-id $ROUTE_TABLE_ID )
if [ "$RESULT" == "" ]; then
	echo "[error] associating Subnet3 with route Table  failed!" 1>&2
	exit 1
fi
echo "Subnet ID '$SUBNET_PUBLIC_ID3' ASSOCIATED with Route Table ID" \
  "'$ROUTE_TABLE_ID'."

group_id=$(aws ec2 describe-security-groups --filters Name=vpc-id,Values=${VPC_ID})
if [ "$group_id" == "" ]; then
	echo "[error] Can't find security group" 1>&2
	exit 1
fi
sgid=$(echo -e "$group_id" | jq '.SecurityGroups[0].GroupId' | tr -d '"')
if [ "$sgid" == "" ]; then
	echo "[error] Can't find security group id" 1>&2
	exit 1
fi
#to revoke security access
response1=$(aws ec2 revoke-security-group-ingress \
 --group-id "$sgid" \
 --protocol all --port all \
 --source-group "$sgid")
if [ "response1" == "" ]; then
	echo "[error] Can't revoke security group ingress rules" 1>&2
	exit 1
fi
response2=$(aws ec2 revoke-security-group-egress \
 --group-id "$sgid" \
 --protocol all --port all \
 --cidr 0.0.0.0/0)
if [ "response2" == "" ]; then
	echo "[error] Can't revoke security group egress rules" 1>&2
	exit 1
fi
#to authorize tcp access on port 22 and 80
response3=$(aws ec2 authorize-security-group-ingress \
 --group-id "$sgid" \
 --protocol tcp \
 --port 22 --cidr 0.0.0.0/0)
if [ "response3" == "" ]; then
	echo "[error] Can't authorize port 22" 1>&2
	exit 1
fi
response4=$(aws ec2 authorize-security-group-ingress \
 --group-id "$sgid" \
 --protocol tcp \
 --port 80 --cidr 0.0.0.0/0)
if [ "response4" == "" ]; then
	echo "[error] Can't authorize port 80" 1>&2
	exit 1
fi

echo "Successfully created VPC, $VPC_NAME"
exit 0
