# AWS EC2 Deployment Guide

This guide explains how to deploy the Test Platform to a single AWS EC2 virtual machine using the provided `docker-compose.yml` configuration. This setup hosts both the Spring Boot backend (serving the static frontend) and the PostgreSQL database on the same server, qualifying for the **AWS Free Tier (t2.micro / t3.micro)**.

---

## Prerequisites
* An active **AWS Account**.
* A terminal client or SSH client installed locally.
* Your project files pushed to a public or private Git repository (e.g., GitHub).
* A free **DuckDNS** account to register a subdomain.

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
     * **HTTP:** Port `80` (Source: *0.0.0.0/0* for HTTP challenges & redirects).
     * **HTTPS:** Port `443` (Source: *0.0.0.0/0* for secure web traffic).
5. Click **Launch Instance**.

---

## Step 1.5: Configure DuckDNS

Since SSL certificates cannot be generated for raw IP addresses, you need to map a domain name to your EC2 IP.

1. Go to [duckdns.org](https://www.duckdns.org/) and log in (e.g. via Google or GitHub).
2. Choose a subdomain (e.g. `testplatform-azhar`) and click **add domain**.
3. Under your domain listing, copy your EC2 instance's **Public IPv4 Address** and paste it into the IP text field next to your subdomain, then click **update ip**.
4. Now, your domain `testplatform-azhar.duckdns.org` points directly to your EC2 instance.

---

## Step 2: Connect to the EC2 Instance

Open your local terminal and navigate to the folder where your download key pair (`testplatform-key.pem`) is located.

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

Once connected to your EC2 instance, install Docker based on the OS you chose:

### Option A: For Amazon Linux 2023 / Amazon Linux 2:
```bash
# Update packages
sudo dnf update -y  # Use "sudo yum update -y" on Amazon Linux 2

# Install Docker and Git
sudo dnf install -y docker git  # Use "sudo yum install -y docker git" on Amazon Linux 2

# Start and enable Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Add ec2-user to the docker group so you don't need sudo
sudo usermod -aG docker ec2-user

# Log out and log back in to apply group changes
exit
```
*After logging out, reconnect via SSH:*
```bash
ssh -i testplatform-key.pem ec2-user@<EC2_PUBLIC_IP>
```

### Option B: For Ubuntu 22.04 LTS:
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
2. Open `docker-compose.yml` on the EC2 instance and change `your-subdomain.duckdns.org` to your actual DuckDNS subdomain:
   ```yaml
      - DUCKDNS_DOMAIN=testplatform-azhar.duckdns.org
   ```
3. Launch the application using Docker Compose:
   ```bash
   docker-compose up -d --build
   ```
   *(Note: If you receive a `requires buildx` error on Amazon Linux, run instead: `DOCKER_BUILDKIT=0 COMPOSE_DOCKER_CLI_BUILD=0 docker-compose up -d --build`)*
   
   *The `--build` flag builds the Spring Boot app from the local `Dockerfile` (copying the frontend assets at compilation).*
   *The `-d` flag runs the containers in detached mode (background).*

---

## Step 5: Access the Application

Caddy will automatically request and install the Let's Encrypt SSL certificate for your DuckDNS domain. Within a minute, your app will be live and secure:

* **Student Portal:** `https://your-subdomain.duckdns.org/index.html`
* **Admin Portal:** `https://your-subdomain.duckdns.org/admin-login.html`

---

## Troubleshooting & Common Commands

* **Check running containers:**
  ```bash
  docker-compose ps
  ```
* **View application logs:**
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
