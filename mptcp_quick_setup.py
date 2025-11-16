#!/usr/bin/env python3
import subprocess
import sys
import time

def run_command(cmd, description, check=True):
    """Run a shell command and print the result"""
    print(f"\n{'='*60}")
    print(f"{description}")
    print(f"{'='*60}")
    print(f"Running: {cmd}")
    
    try:
        result = subprocess.run(
            cmd,
            shell=True,
            capture_output=True,
            text=True,
            check=check
        )
        if result.stdout:
            print(result.stdout)
        if result.stderr:
            print(f"Warning: {result.stderr}", file=sys.stderr)
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"Error: {e}", file=sys.stderr)
        if e.stdout:
            print(e.stdout)
        if e.stderr:
            print(e.stderr, file=sys.stderr)
        return None

def get_network_interfaces():
    """Get all network interfaces with IPv4 addresses (excluding loopback)"""
    result = run_command("ip -4 -o addr show", "Scanning network interfaces", check=False)
    
    interfaces = []
    if result:
        for line in result.strip().split('\n'):
            parts = line.split()
            if len(parts) >= 4:
                # Extract interface name and IP
                iface = parts[1]
                ip_with_cidr = parts[3]
                ip = ip_with_cidr.split('/')[0]
                
                # Skip loopback (127.x.x.x)
                if not ip.startswith('127.'):
                    interfaces.append({'device': iface, 'ip': ip})
    
    return interfaces

def kill_existing_connections():
    """Kill existing iperf3 and mptcpize processes, but preserve Spring Boot app"""
    print("\n" + "="*60)
    print("Killing existing connections and releasing ports")
    print("="*60)
    
    # Kill iperf3
    run_command("sudo killall iperf3", "Killing iperf3 processes", check=False)
    time.sleep(0.5)
    
    # Kill mptcpize
    run_command("sudo killall mptcpize", "Killing mptcpize processes", check=False)
    time.sleep(0.5)
    
    # DON'T kill port 5000 - Spring Boot backend needs it
    # The socket will be released when transmission stops naturally
    
    print("Existing connections terminated")

def configure_mptcp():
    """Configure MPTCP settings"""
    print("\n" + "="*60)
    print("MPTCP COMPLETE CONFIGURATION SCRIPT")
    print("="*60)
    
    # Step 1: Kill existing connections
    kill_existing_connections()
    
    # Step 2: Get network interfaces
    interfaces = get_network_interfaces()
    
    if not interfaces:
        print("\nError: No network interfaces found!")
        sys.exit(1)  # Exit with error code
    
    print(f"\nFound {len(interfaces)} network interface(s):")
    for iface in interfaces:
        print(f"  - {iface['device']}: {iface['ip']}")
    
    # Step 3: Flush all existing MPTCP endpoints
    run_command(
        "sudo ip mptcp endpoint flush",
        "Flushing all existing MPTCP endpoints"
    )
    
    # Step 4: Add all interfaces as signal endpoints
    print("\n" + "="*60)
    print("Adding endpoints with 'signal' flag")
    print("="*60)
    
    for iface in interfaces:
        run_command(
            f"sudo ip mptcp endpoint add {iface['ip']} dev {iface['device']} signal",
            f"Adding {iface['ip']} ({iface['device']}) as signal endpoint"
        )
    
    # Step 5: Set MPTCP scheduler
    run_command(
        "sudo sysctl -w net.mptcp.scheduler=clientportsched",
        "Setting MPTCP scheduler to clientportsched"
    )
    
    # Step 6: Set MPTCP limits
    run_command(
        "sudo ip mptcp limits set add_addr_accepted 2",
        "Setting MPTCP limits (add_addr_accepted=2)"
    )
    
    # Step 7: Show final configuration
    print("\n" + "="*60)
    print("FINAL MPTCP CONFIGURATION")
    print("="*60)
    
    run_command("ip mptcp endpoint show", "Current MPTCP Endpoints")
    run_command("ip mptcp limits show", "Current MPTCP Limits")
    run_command("sysctl net.mptcp.scheduler", "Current MPTCP Scheduler")
    
    print("\n" + "="*60)
    print("Configuration complete!")
    print("="*60)
    print("\nReady for MPTCP connections")
    print("="*60)
    
    # Exit successfully
    sys.exit(0)

if __name__ == "__main__":
    try:
        configure_mptcp()
    except KeyboardInterrupt:
        print("\n\nScript interrupted by user.")
        sys.exit(1)
    except Exception as e:
        print(f"\nUnexpected error: {e}", file=sys.stderr)
        sys.exit(1)