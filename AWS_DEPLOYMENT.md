# AWS EC2 Deployment Guide

This guide explains how to deploy the Test Platform to a single AWS EC2 virtual machine using the provided `docker-compose.yml` configuration. This setup hosts both the Spring Boot backend (serving the static frontend) and the PostgreSQL database on the same server, qualifying for the **AWS Free Tier (t2.micro / t3.micro)**.

---

## Prerequisites
* An active **AWS Account**.
* A terminal client or SSH client installed locally.
* Your project files pushed to a Git repository (e.g., GitHub).

---

## Step 1: Launch an EC2 Instance

1. Log in to the [AWS Management Console](https://aws.amazon.com/console/).
2. Navigate to the **EC2 Dashboard** and click **Launch Instance**.
3. Configure the instance details:
   * **Name:** `test-platform-server`
   * **OS (Amazon Machine Image):** Select **Ubuntu 22.04 LTS** (or **Amazon Linux 2023**).
   * **Instance Type:** Select **t2.micro** (1 GiB RAM, Free Tier Eligible) or **t3.micro** depending on region.
   * **Key Pair:** Create a new key pair (e.g., `testplatform-key.pem`) and download it. Store it safely!
4. **Network Settings (Security Group):**
   * Select/Create a Security Group.
   * Add the following inbound rules:
     * **SSH:** Port `22` (Source: *My IP* or *0.0.0.0/0* to access terminal).
     * **HTTP:** Port `80` (Source: *0.0.0.0/0* for HTTP web traffic).
5. Click **Launch Instance**.

---

## Step 2: Connect to the EC2 Instance

Open your local terminal and navigate to the folder where your downloaded key pair (`testplatform-key.pem`) is located.

1. Set the correct file permissions on the key:
   ```bash
   chmod 400 testplatform-key.pem
   ```
2. Connect to the EC2 instance using its **Public IPv4 Address** (found in the EC2 dashboard):
   ```bash
   ssh -i testplatform-key.pem ubuntu@<EC2_PUBLIC_IP>
   ```
   *(Note: Use `ec2-user@<EC2_PUBLIC_IP>` if you launched Amazon Linux).*

---

## Step 3: Install Docker and Docker Compose

Once connected to your EC2 instance, install Docker:

### For Ubuntu 22.04 LTS:
```bash
# Update packages
sudo apt-get update -y
sudo apt-get upgrade -y

# Install Docker and Git
sudo apt-get install -y docker.io git

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Add your user to the docker group so you don't need sudo
sudo usermod -aG docker $USER

# Log out and log back in to apply group changes
exit
```

*After logging out, reconnect via SSH:*
```bash
ssh -i testplatform-key.pem ubuntu@<EC2_PUBLIC_IP>
```

---

## Step 4: Clone the Repo and Run the App

1. Clone your GitHub repository to the instance:
   ```bash
   git clone https://github.com/azharkhan924/TestYourKnowledge.git
   cd TestYourKnowledge
   ```
2. Launch the application using Docker Compose:
   ```bash
   docker-compose up -d --build
   ```
   *(Note: If you receive a `requires buildx` error on Amazon Linux, run instead: `DOCKER_BUILDKIT=0 COMPOSE_DOCKER_CLI_BUILD=0 docker-compose up -d --build`)*
   
   *The `--build` flag builds the Spring Boot app from the local `Dockerfile` (copying the frontend assets at compilation).*
   *The `-d` flag runs the containers in detached mode (background).*

---

## Step 5: Access the Application

Once the containers are running, the application will be accessible directly via your EC2 instance's Public IP over plain HTTP:

* **Student Portal / Landing Page:** `http://<EC2_PUBLIC_IP>/index.html`
* **Teacher Portal:** `http://<EC2_PUBLIC_IP>/admin-login.html`

---

## Troubleshooting & Common Commands

* **Check running containers:**
  ```bash
  docker-compose ps
  ```
* **View application logs (to check OTP codes printed in console):**
  ```bash
  docker-compose logs -f web-app
  ```
* **Stop the application:**
  ```bash
  docker-compose down
  ```
* **Restart the application:**
  ```bash
  docker-compose restart
  ```
