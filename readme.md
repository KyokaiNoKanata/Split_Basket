# Split Basket - Smart Shopping & Life Management App

Split Basket is an intelligent life management application that integrates shopping list, inventory
management, and bill splitting functions, helping users easily manage family shopping and living
expenses.

## Main Features

### 1. Shopping List

- Create and manage shopping lists
- Quickly add items
- Check items as purchased
- Quickly select frequently used items from recommended lists

### 2. Inventory Management

- Manually add inventory items
- Automatically import purchased items from shopping lists
- Item category management (Vegetables, Meat, Fruits, Others)
- Inventory overview statistics (Remaining quantity, Expiring soon, Consumed)
- Item expiration reminders

### 3. Bill Splitting

- Create and manage bills
- Support multiple splitting methods (Equal, By Quantity, By Item, Custom)
- Bill status management (Unpaid/Paid)
- Member management functionality
- Unpaid bill reminders

### 4. Home Screen Reminders

- Reminders for all unpaid bills
- Reminders for all soon-to-expire items
- Display all reminders using RecyclerView

## Technology Architecture

### Core Components

- **HomeActivity.java** - Application main interface
- **InventoryActivity.java** - Inventory management interface
- **ListActivity.java** - Shopping list interface
- **BillActivity.java** - Bill splitting interface
- **ReminderAdapter.java** - Reminder list adapter
- **StatusLogAdapter.java** - Log list adapter

### Data Storage

- Local data persistence using Room database
- Local storage for items, shopping lists, bills, etc.

### Interface Design

- Material Design style
- Responsive layout
- Smooth page transition animations
- Bottom navigation bar for quick module switching

## Project Structure

```
app/
├── src/
│   └── main/
│       ├── java/com/example/split_basket/
│       │   ├── data/              # Data layer
│       │   ├── HomeActivity.java  # Main interface
│       │   ├── InventoryActivity.java  # Inventory management
│       │   ├── ListActivity.java  # Shopping list
│       │   ├── BillActivity.java  # Bill splitting
│       │   ├── ReminderAdapter.java  # Reminder adapter
│       │   └── StatusLogAdapter.java  # Log adapter
│       └── res/
│           ├── layout/            # Layout files
│           ├── values/            # Resource files
│           └── drawable/          # Image resources
└── build.gradle.kts                # Project configuration
```

## Quick Start

### Environment Requirements

- Android Studio 4.0+
- Android SDK API Level 23+

### Compilation and Running

1. Clone the project locally
2. Open Android Studio
3. Import the project and wait for gradle synchronization to complete
4. Connect an Android device or start an emulator
5. Click the run button

## Usage Instructions

### Adding a Shopping List

1. Click the "New List" card on the main interface
2. Enter the list name
3. Add items you need to buy
4. Click the finish button to save

### Managing Inventory

1. Click the "Inventory" icon in the bottom navigation bar on the main interface
2. Click the "Add Item" button to manually add items
3. Or import purchased items from the shopping list
4. You can filter and view items by category

### Creating a Bill

1. Click the "New Bill" card on the main interface
2. Enter the bill name and total amount
3. Select the splitting method
4. Select participating members
5. Save the bill

### Viewing Reminders

Check all unpaid bills and soon-to-expire items in the "Reminder" section on the main interface

