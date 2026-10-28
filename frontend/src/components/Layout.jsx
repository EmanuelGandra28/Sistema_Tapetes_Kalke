import {NavLink, useNavigate} from 'react-router-dom';
import {
    BarChart3,
    Box,
    ClipboardList,
    Factory,
    LayoutDashboard,
    LogOut,
    Settings,
    ShoppingBag,
    Truck,
    Users,
    WalletCards
} from 'lucide-react';
import {useAuth} from '../contexts/AuthContext';

const links = [['dashboard', '/dashboard', 'Dashboard', LayoutDashboard], ['clientes', '/clientes', 'Clientes', Users], ['funcionarios', '/funcionarios', 'Funcionários', Users], ['produtos', '/produtos', 'Estoque', Box], ['vendas', '/vendas', 'Vendas', ShoppingBag], ['compras', '/compras', 'Compras', WalletCards], ['fornecedores', '/fornecedores', 'Fornecedores', Users], ['producao', '/ordens-producao', 'Produção', Factory], ['entregas', '/entregas', 'Entregas', Truck], ['relatorios', '/relatorios', 'Relatórios', BarChart3], ['configuracoes', '/configuracoes', 'Configurações', Settings]];
export default function Layout({children}) {
    const {user, canPage, logout} = useAuth();
    const navigate = useNavigate();
    const leave = async () => {
        await logout();
        navigate('/login');
    };
    return <div className="app-shell">
        <aside>
            <div className="brand">Tapetes <strong>Kalke</strong></div>
            <nav>{links.filter(([key]) => canPage(key)).map(([key, to, label, Icon]) => <NavLink key={key} to={to}><Icon
                size={18}/><span>{label}</span></NavLink>)}</nav>
            <div className="account"><span>{user?.nome}</span><small>{user?.cargo}</small>
                <button onClick={leave}><LogOut size={16}/> Sair</button>
            </div>
        </aside>
        <div className="content">
            <header className="topbar"><span><ClipboardList size={18}/> Gestão operacional</span></header>
            <main>{children}</main>
        </div>
    </div>;
}