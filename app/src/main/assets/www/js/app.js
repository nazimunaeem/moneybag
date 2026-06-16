const { createApp } = Vue

createApp({
    data() {
        return {
            view: 'transactions',
            modal: null,
            transactions: [],
            accounts: [],
            categories: [],
            form: {
                transaction: { type: 'EXPENSE', amount: null, accountId: null, category: '', note: '' },
                account: { name: '', balance: 0, type: 'CASH' },
                category: { emoji: '', name: '', type: 'EXPENSE' }
            }
        }
    },
    computed: {
        sortedTransactions() {
            return [...this.transactions].sort((a, b) => b.transaction.timestamp - a.transaction.timestamp)
        },
        totalIncome() {
            return this.transactions
                .filter(t => t.transaction.type === 'INCOME')
                .reduce((sum, t) => sum + t.transaction.amount, 0)
        },
        totalExpense() {
            return this.transactions
                .filter(t => t.transaction.type === 'EXPENSE')
                .reduce((sum, t) => sum + t.transaction.amount, 0)
        },
        netBalance() {
            return this.totalIncome - this.totalExpense
        },
        modalTitle() {
            switch(this.modal) {
                case 'addTransaction': return 'Add Transaction'
                case 'addAccount': return 'Add Account'
                case 'editAccount': return 'Modify Balance'
                case 'addCategory': return 'Add Category'
                default: return ''
            }
        },
        filteredCategories() {
            return this.categories.filter(c => c.type === this.form.transaction.type)
        }
    },
    mounted() {
        this.fetchData()
    },
    methods: {
        async fetchData() {
            try {
                const [tRes, aRes, cRes] = await Promise.all([
                    fetch('/api/transactions'),
                    fetch('/api/accounts'),
                    fetch('/api/categories')
                ])
                this.transactions = await tRes.json()
                this.accounts = await aRes.json()
                this.categories = await cRes.json()

                if (this.accounts.length > 0) this.form.transaction.accountId = this.accounts[0].id
                if (this.filteredCategories.length > 0) this.form.transaction.category = this.filteredCategories[0].name
            } catch (e) {
                console.error("Failed to fetch data", e)
            }
        },
        showModal(type) {
            this.modal = type
            if (type === 'addTransaction') {
                if (this.accounts.length > 0) this.form.transaction.accountId = this.accounts[0].id
            }
        },
        async saveTransaction() {
            const data = {
                ...this.form.transaction,
                title: this.form.transaction.category,
                timestamp: Date.now(),
                currency: 'BDT'
            }
            if (await this.post('/api/transactions', data)) {
                this.modal = null
                this.fetchData()
                alert("Transaction Saved!")
            }
        },
        async saveAccount() {
            const data = { ...this.form.account, currency: 'BDT' }
            if (await this.post('/api/accounts', data)) {
                this.modal = null
                this.fetchData()
                alert("Account Saved!")
            }
        },
        async saveCategory() {
            const name = this.form.category.emoji
                ? `${this.form.category.emoji} ${this.form.category.name}`
                : this.form.category.name
            const data = { name, type: this.form.category.type }
            if (await this.post('/api/categories', data)) {
                this.modal = null
                this.fetchData()
                alert("Category Saved!")
            }
        },
        editAccount(account) {
            this.form.account = { ...account }
            this.modal = 'editAccount'
        },
        async post(url, data) {
            try {
                const res = await fetch(url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                })
                return res.ok
            } catch (e) {
                alert("Operation failed: " + e.message)
                return false
            }
        },
        formatDate(timestamp) {
            const date = new Date(timestamp)
            return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
    }
}).mount('#app')
